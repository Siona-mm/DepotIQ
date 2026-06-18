"""Generate a reproducible synthetic retail demand dataset for DepotIQ.

The output is intentionally synthetic and must be documented as such. It contains
learnable demand patterns for leakage-safe 1, 3, 7, 14, and 30-day forecasting.
"""

from __future__ import annotations

import argparse
from collections import deque
from pathlib import Path

import numpy as np
import pandas as pd


SEED = 42
START_DATE = "2023-01-01"
END_DATE = "2024-02-04"
DEFAULT_STORE_COUNT = 10
DEFAULT_PRODUCT_COUNT = 25
CHUNK_SIZE = 50_000
OUTPUT_PATH = Path(__file__).resolve().parents[2] / "data" / "synthetic" / "depotiq_synthetic_retail.csv"

CATEGORIES = [
    "Groceries", "Beverages", "Clothing", "Electronics", "Furniture",
    "Toys", "Personal Care", "Household", "Sports", "Office Supplies",
]
REGIONS = ["North", "South", "East", "West", "Central"]

CATEGORY_SETTINGS = {
    "Groceries": {"demand": (18, 65), "price": (1.20, 24), "season_amp": 0.10, "elasticity": 1.4, "weekend": 1.18, "shelf": (4, 180), "weight": (0.15, 5.0), "perishable": 0.72},
    "Beverages": {"demand": (14, 52), "price": (0.80, 30), "season_amp": 0.20, "elasticity": 1.5, "weekend": 1.24, "shelf": (30, 540), "weight": (0.25, 12.0), "perishable": 0.25},
    "Clothing": {"demand": (5, 26), "price": (8, 180), "season_amp": 0.25, "elasticity": 1.8, "weekend": 1.22, "shelf": (730, 3650), "weight": (0.08, 2.5), "perishable": 0.0},
    "Electronics": {"demand": (2, 16), "price": (12, 1400), "season_amp": 0.30, "elasticity": 2.0, "weekend": 1.17, "shelf": (730, 3650), "weight": (0.05, 18.0), "perishable": 0.0},
    "Furniture": {"demand": (1, 10), "price": (35, 2200), "season_amp": 0.18, "elasticity": 1.6, "weekend": 1.12, "shelf": (1825, 7300), "weight": (2.0, 95.0), "perishable": 0.0},
    "Toys": {"demand": (3, 24), "price": (3, 220), "season_amp": 0.42, "elasticity": 1.7, "weekend": 1.28, "shelf": (730, 3650), "weight": (0.05, 8.0), "perishable": 0.0},
    "Personal Care": {"demand": (7, 32), "price": (1.50, 95), "season_amp": 0.08, "elasticity": 1.4, "weekend": 1.14, "shelf": (180, 1095), "weight": (0.03, 2.0), "perishable": 0.05},
    "Household": {"demand": (5, 28), "price": (2, 160), "season_amp": 0.12, "elasticity": 1.3, "weekend": 1.16, "shelf": (365, 3650), "weight": (0.10, 15.0), "perishable": 0.0},
    "Sports": {"demand": (2, 18), "price": (5, 850), "season_amp": 0.30, "elasticity": 1.7, "weekend": 1.26, "shelf": (730, 3650), "weight": (0.05, 35.0), "perishable": 0.0},
    "Office Supplies": {"demand": (4, 25), "price": (0.50, 260), "season_amp": 0.22, "elasticity": 1.5, "weekend": 0.86, "shelf": (730, 3650), "weight": (0.01, 12.0), "perishable": 0.0},
}

BRANDS = ["Northfield", "Aster", "Everline", "Mosaic", "Creston", "Oak & Co", "Nova", "Clearway", "Summit", "DailyChoice"]
PRODUCT_NOUNS = {
    "Groceries": ["Rice", "Pasta", "Bread", "Cheese", "Yogurt", "Cereal", "Eggs", "Frozen Meal"],
    "Beverages": ["Mineral Water", "Orange Juice", "Coffee", "Tea", "Sparkling Water", "Energy Drink"],
    "Clothing": ["T-Shirt", "Jacket", "Jeans", "Sweater", "Socks", "Running Shorts"],
    "Electronics": ["Headphones", "Keyboard", "Smart Speaker", "Monitor", "Charger", "Tablet"],
    "Furniture": ["Desk", "Chair", "Bookshelf", "Coffee Table", "Cabinet", "Bedside Table"],
    "Toys": ["Building Set", "Puzzle", "Board Game", "Doll", "Model Car", "Craft Kit"],
    "Personal Care": ["Shampoo", "Soap", "Toothpaste", "Face Cream", "Deodorant", "Body Lotion"],
    "Household": ["Detergent", "Storage Box", "Kitchen Towel", "Light Bulb", "Cleaning Spray", "Cookware Set"],
    "Sports": ["Yoga Mat", "Football", "Dumbbell", "Water Bottle", "Resistance Band", "Backpack"],
    "Office Supplies": ["Notebook", "Pen Set", "Printer Paper", "Desk Organizer", "Stapler", "Marker Set"],
}


STORE_TYPES = [
    {"Store Type": "Small", "Has Warehouse": 0, "capacity_per_product": 130,
     "Delivery Lead Time": 1, "Preferred Horizon Days": 3, "factor_range": (0.65, 0.88)},
    {"Store Type": "Medium", "Has Warehouse": 0, "capacity_per_product": 210,
     "Delivery Lead Time": 2, "Preferred Horizon Days": 7, "factor_range": (0.88, 1.10)},
    {"Store Type": "Large", "Has Warehouse": 1, "capacity_per_product": 360,
     "Delivery Lead Time": 4, "Preferred Horizon Days": 14, "factor_range": (1.08, 1.32)},
    {"Store Type": "Warehouse Store", "Has Warehouse": 1, "capacity_per_product": 620,
     "Delivery Lead Time": 6, "Preferred Horizon Days": 30, "factor_range": (1.25, 1.55)},
]


def build_store_profiles(store_count: int, product_count: int, rng: np.random.Generator) -> list[dict]:
    stores = []
    for index in range(store_count):
        template = STORE_TYPES[index % len(STORE_TYPES)]
        stores.append(
            {
                "Store ID": f"S{index + 1:03d}",
                "Region": REGIONS[index % len(REGIONS)],
                "Store Type": template["Store Type"],
                "Has Warehouse": template["Has Warehouse"],
                "Storage Capacity": template["capacity_per_product"] * product_count,
                "Delivery Lead Time": template["Delivery Lead Time"],
                "Preferred Horizon Days": template["Preferred Horizon Days"],
                "factor": rng.uniform(*template["factor_range"]),
            }
        )
    return stores


def build_products(product_count: int, rng: np.random.Generator) -> list[dict]:
    products = []
    for index in range(product_count):
        category = CATEGORIES[index % len(CATEGORIES)]
        settings = CATEGORY_SETTINGS[category]
        brand = BRANDS[int(rng.integers(0, len(BRANDS)))]
        noun = PRODUCT_NOUNS[category][int(rng.integers(0, len(PRODUCT_NOUNS[category])))]
        base_price = float(np.exp(rng.uniform(np.log(settings["price"][0]), np.log(settings["price"][1]))))
        margin = rng.uniform(0.22, 0.58)
        products.append(
            {
                "Product ID": f"P{index + 1:04d}",
                "Product Name": f"{brand} {noun} {index + 1}",
                "Brand": brand,
                "Category": category,
                "Supplier ID": f"SUP{int(rng.integers(1, 16)):03d}",
                "base_demand": rng.uniform(*settings["demand"]),
                "base_price": base_price,
                "unit_cost": base_price * (1 - margin),
                "weight_kg": float(np.exp(rng.uniform(np.log(settings["weight"][0]), np.log(settings["weight"][1])))),
                "shelf_life": int(rng.integers(settings["shelf"][0], settings["shelf"][1] + 1)),
                "perishable": int(rng.random() < settings["perishable"]),
                "trend": rng.uniform(-0.00012, 0.00030),
                "phase": rng.uniform(0, 2 * np.pi),
            }
        )
    return products


def holiday_name(date: pd.Timestamp) -> str | None:
    month_day = (date.month, date.day)
    if month_day in {(12, 24), (12, 25), (12, 26)}:
        return "Christmas"
    if month_day in {(11, 24), (11, 25), (11, 26), (11, 27), (11, 28), (11, 29), (11, 30)}:
        return "November Sale"
    if month_day == (1, 1):
        return "New Year"
    return None


def weather_for_day(date: pd.Timestamp, region_index: int, rng: np.random.Generator) -> tuple[str, float]:
    annual = np.sin(2 * np.pi * (date.dayofyear - 80) / 365.25)
    region_offset = [0, 5, 2, 1, 3][region_index % len(REGIONS)]
    temperature = 14 + 11 * annual + region_offset + rng.normal(0, 2.5)
    rain_probability = 0.30 - 0.10 * annual
    draw = rng.random()
    if temperature < 1 and draw < 0.35:
        condition = "Snowy"
    elif draw < rain_probability:
        condition = "Rainy"
    elif draw > 0.78:
        condition = "Cloudy"
    else:
        condition = "Sunny"
    return condition, round(float(temperature), 1)


def generate_dataset(store_count: int, product_count: int, output_path: Path) -> dict[str, int]:
    rng = np.random.default_rng(SEED)
    dates = pd.date_range(START_DATE, END_DATE, freq="D")
    stores = build_store_profiles(store_count, product_count, rng)
    products = build_products(product_count, rng)
    rows: list[dict] = []
    rows_written = 0
    stockout_rows = 0
    wrote_header = False

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.unlink(missing_ok=True)

    inventory: dict[tuple[str, str], int] = {}
    pipelines: dict[tuple[str, str], deque] = {}
    recent_demand: dict[tuple[str, str], deque] = {}

    for store in stores:
        per_product_capacity = int(store["Storage Capacity"] / len(products))
        for product in products:
            key = (store["Store ID"], product["Product ID"])
            start_level = int(per_product_capacity * rng.uniform(0.55, 0.85))
            inventory[key] = start_level
            pipelines[key] = deque([0] * (store["Delivery Lead Time"] + 1))
            recent_demand[key] = deque(maxlen=30)

    for day_index, date in enumerate(dates):
        holiday = holiday_name(date)
        for store_index, store in enumerate(stores):
            weather, temperature = weather_for_day(date, store_index, rng)
            per_product_capacity = int(store["Storage Capacity"] / len(products))

            for product in products:
                key = (store["Store ID"], product["Product ID"])
                settings = CATEGORY_SETTINGS[product["Category"]]

                arriving = pipelines[key].popleft()
                inventory[key] = min(per_product_capacity, inventory[key] + arriving)
                start_inventory = inventory[key]

                promo_start = rng.random() < 0.018
                product_number = int(product["Product ID"][1:])
                recurring_campaign = product_number % 25 in {2, 7, 12, 17}
                promotion = int(promo_start or (day_index % 91 in range(12, 16) and recurring_campaign))
                discount = int(rng.choice([10, 15, 20, 30], p=[0.30, 0.30, 0.28, 0.12])) if promotion else 0

                inflation = 1 + 0.025 * (day_index / 365.25)
                price_cycle = 1 + 0.025 * np.sin(2 * np.pi * day_index / 90)
                local_price_noise = rng.normal(1.0, 0.012)
                regular_price = product["base_price"] * inflation * price_cycle * local_price_noise
                price = regular_price * (1 - discount / 100)
                competitor_price = regular_price * rng.normal(1.02, 0.055)
                relative_price = price / max(competitor_price, 0.01)

                yearly = 1 + settings["season_amp"] * np.sin(2 * np.pi * date.dayofyear / 365.25 + product["phase"])
                weekday = settings["weekend"] if date.dayofweek >= 5 else (0.92 if date.dayofweek == 0 else 1.0)
                growth = max(0.78, 1 + product["trend"] * day_index)
                promo_effect = 1 + (discount / 100) * settings["elasticity"] if promotion else 1.0
                price_effect = np.clip(relative_price ** (-settings["elasticity"]), 0.65, 1.55)

                holiday_effect = 1.0
                if holiday:
                    holiday_effect = {
                        "Toys": 1.85,
                        "Electronics": 1.55,
                        "Clothing": 1.30,
                        "Groceries": 1.22,
                        "Furniture": 1.12,
                        "Beverages": 1.20,
                        "Personal Care": 1.16,
                        "Household": 1.14,
                        "Sports": 1.22,
                        "Office Supplies": 0.86,
                    }[product["Category"]]

                weather_effect = 1.0
                if weather in {"Rainy", "Snowy"}:
                    weather_effect *= 0.91
                if product["Category"] == "Groceries" and temperature > 25:
                    weather_effect *= 1.10
                if product["Category"] == "Clothing" and temperature < 5:
                    weather_effect *= 1.12

                expected_demand = (
                    product["base_demand"]
                    * store["factor"]
                    * yearly
                    * weekday
                    * growth
                    * promo_effect
                    * price_effect
                    * holiday_effect
                    * weather_effect
                )
                # Unobserved day-to-day variation keeps the task realistic while
                # preserving the underlying weekly, seasonal, and promotion signal.
                demand_shock = rng.lognormal(mean=-(0.28**2) / 2, sigma=0.28)
                expected_demand *= demand_shock
                expected_demand = max(0.5, expected_demand)
                true_demand = int(rng.poisson(expected_demand))
                units_sold = min(true_demand, start_inventory)
                stockout_units = true_demand - units_sold
                stockout_rows += int(stockout_units > 0)
                inventory[key] -= units_sold
                recent_demand[key].append(true_demand)

                history = list(recent_demand[key])
                historical_daily = float(np.mean(history)) if history else expected_demand
                horizon = store["Preferred Horizon Days"]
                safety_days = max(1, round(horizon * 0.15))
                target_stock = min(per_product_capacity, round(historical_daily * (horizon + safety_days)))
                pipeline_total = sum(pipelines[key])
                units_ordered = max(0, target_stock - inventory[key] - pipeline_total)
                units_ordered = min(units_ordered, per_product_capacity - inventory[key] - pipeline_total)
                pipelines[key].append(int(units_ordered))

                naive_forecast = max(0, historical_daily * rng.normal(1.0, 0.08))

                rows.append(
                    {
                        "Date": date.strftime("%Y-%m-%d"),
                        "Store ID": store["Store ID"],
                        "Product ID": product["Product ID"],
                        "Product Name": product["Product Name"],
                        "Brand": product["Brand"],
                        "Category": product["Category"],
                        "Supplier ID": product["Supplier ID"],
                        "Unit Cost": round(float(product["unit_cost"] * inflation), 2),
                        "Weight Kg": round(product["weight_kg"], 3),
                        "Shelf Life Days": product["shelf_life"],
                        "Perishable": product["perishable"],
                        "Region": store["Region"],
                        "Store Type": store["Store Type"],
                        "Has Warehouse": store["Has Warehouse"],
                        "Storage Capacity": store["Storage Capacity"],
                        "Delivery Lead Time": store["Delivery Lead Time"],
                        "Preferred Horizon Days": horizon,
                        "Inventory Level": start_inventory,
                        "Units Sold": units_sold,
                        "True Demand": true_demand,
                        "Stockout Units": stockout_units,
                        "Units Ordered": int(units_ordered),
                        "Incoming Units": int(arriving),
                        "Demand Forecast": round(naive_forecast, 2),
                        "Price": round(float(price), 2),
                        "Discount": discount,
                        "Weather Condition": weather,
                        "Temperature": temperature,
                        "Holiday/Promotion": int(bool(holiday) or promotion),
                        "Promotion": promotion,
                        "Holiday Name": holiday or "No Holiday",
                        "Competitor Pricing": round(float(competitor_price), 2),
                        "Seasonality": ["Winter", "Spring", "Summer", "Autumn"][(date.month % 12) // 3],
                        "Synthetic Data": 1,
                    }
                )

                if len(rows) >= CHUNK_SIZE:
                    pd.DataFrame(rows).to_csv(
                        output_path,
                        mode="a",
                        header=not wrote_header,
                        index=False,
                    )
                    rows_written += len(rows)
                    wrote_header = True
                    rows.clear()

    if rows:
        pd.DataFrame(rows).to_csv(
            output_path,
            mode="a",
            header=not wrote_header,
            index=False,
        )
        rows_written += len(rows)

    return {"rows": rows_written, "stockout_rows": stockout_rows}


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate the DepotIQ synthetic retail dataset.")
    parser.add_argument("--stores", type=int, default=DEFAULT_STORE_COUNT)
    parser.add_argument("--products", type=int, default=DEFAULT_PRODUCT_COUNT)
    parser.add_argument("--output", type=Path, default=OUTPUT_PATH)
    args = parser.parse_args()

    if args.stores < 1 or args.products < 1:
        parser.error("--stores and --products must both be positive")

    stats = generate_dataset(args.stores, args.products, args.output)

    print("DepotIQ synthetic dataset generated")
    print(f"Rows: {stats['rows']:,}")
    print(f"Dates: {START_DATE} to {END_DATE}")
    print(f"Stores: {args.stores}")
    print(f"Products: {args.products}")
    print(f"Stockout rate: {stats['stockout_rows'] / stats['rows']:.2%}")
    print(f"Saved to: {args.output.resolve()}")


if __name__ == "__main__":
    main()
