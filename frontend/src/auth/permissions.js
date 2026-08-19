export function hasRole(user, role) {
  return user?.roles?.includes(`ROLE_${role}`) ?? false;
}

export function permissionsFor(user) {
  const isAdmin = hasRole(user, "ADMIN");
  const isManager = hasRole(user, "MANAGER");

  return {
    canImportData: isAdmin,
    canManageCatalog: isAdmin,
    canManageInventory: isAdmin || isManager,
    canManageRecommendations: isAdmin || isManager,
    canManageSettings: isAdmin,
    canPlanShipments: isAdmin || isManager,
    isReadOnly: !isAdmin && !isManager,
  };
}
