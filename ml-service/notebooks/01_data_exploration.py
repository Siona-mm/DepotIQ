import pandas as pd

DATA_PATH = "../../data/raw/retail_store_inventory.csv"

df = pd.read_csv(DATA_PATH)

print("First 5 rows:")
print(df.head())

print("\nColumns:")
print(df.columns.tolist())

print("\nDataset info:")
print(df.info())

print("\nBasic statistics:")
print(df.describe())