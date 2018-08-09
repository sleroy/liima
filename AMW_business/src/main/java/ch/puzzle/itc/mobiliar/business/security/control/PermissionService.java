/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.security.control;

import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.*;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import org.apache.commons.lang.StringUtils;

import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static ch.puzzle.itc.mobiliar.business.security.entity.Action.ALL;
import static ch.puzzle.itc.mobiliar.business.security.entity.Action.UPDATE;

@Stateless
public class PermissionService implements Serializable {

    @Inject
    PermissionRepository permissionRepository;

    @Inject
    Logger log;

    @Inject
    SessionContext sessionContext;

    // map containing Roles with Restrictions (legacy & non legacy)
    static Map<String, List<RestrictionDTO>> rolesWithRestrictions;
    // map containing UserRestrictions with Restrictions (non legacy)
    static Map<String, List<RestrictionEntity>> userRestrictions = new ConcurrentHashMap<>();

    @Schedule(hour = "*", minute = "*/20", persistent = false)
	public void reloadRestrctionCache() {
		this.reloadRolesWithRestrictionsList();
		this.resetUserRestrictionList();
	}

    /**
     * Checks if the caller is allowed to see Deployments
     */
    public boolean hasPermissionToSeeDeployment() {
        return hasPermission(Permission.DEPLOYMENT, null, Action.READ, null, null);
    }

    /**
     * Checks if the caller is allowed to create (re-)Deployments
     */
    public boolean hasPermissionToCreateDeployment() {
        return hasPermission(Permission.DEPLOYMENT, null, Action.CREATE, null, null);
    }

    /**
     * Checks if the caller is allowed to edit Deployments
     */
    public boolean hasPermissionToEditDeployment() {
        return hasPermission(Permission.DEPLOYMENT, null, Action.UPDATE, null, null);
    }

    public boolean hasPermissionForDeploymentUpdate(DeploymentEntity deployment) {
        return hasPermissionAndActionForDeploymentOnContext(deployment.getContext(), deployment.getResource().getResourceGroup(), Action.UPDATE);
    }

    public boolean hasPermissionForDeploymentCreation(DeploymentEntity deployment) {
        return hasPermissionAndActionForDeploymentOnContext(deployment.getContext(), deployment.getResource().getResourceGroup(), Action.CREATE);
    }

    public boolean hasPermissionForDeploymentReject(DeploymentEntity deployment) {
        return hasPermissionAndActionForDeploymentOnContext(deployment.getContext(), deployment.getResource().getResourceGroup(), Action.DELETE);
    }

    public boolean hasPermissionForCancelDeployment(DeploymentEntity deployment) {
        // cancel is only for requester
        return getCurrentUserName().equals(deployment.getDeploymentRequestUser());
    }

    /**
     * Checks if the caller is allowed to perform the requested action for specific ResourceGroup on the specific Environment
     * Note: Both, Permission/Restriction by Group and by User are checked
     *
     * @param context
     * @param resourceGroup
     * @param action
     */
    public boolean hasPermissionAndActionForDeploymentOnContext(ContextEntity context, ResourceGroupEntity resourceGroup, Action action) {
        return hasPermission(Permission.DEPLOYMENT, context, action, resourceGroup, null);
    }

    /**
     * Returns all available Roles with their Restrictions
     * Legacy Permissions are mapped to the new Permission/Restriction model
     *
     * @return Map key=Role.name, value=RestrictionDTOs
     */
    public Map<String, List<RestrictionDTO>> getRolesWithRestrictions() {
        if (rolesWithRestrictions == null) {
            this.reloadRolesWithRestrictionsList();
        }
        return rolesWithRestrictions;
    }

    void reloadRolesWithRestrictionsList() {
        Map<String, List<RestrictionDTO>> tmpRolesWithRestrictions = new HashMap<>();
        // add new permissions with restriction
        if (permissionRepository.getRolesWithRestrictions() != null) {
            for (RoleEntity role : permissionRepository.getRolesWithRestrictions()) {
                addPermission(tmpRolesWithRestrictions, role);
            }
        }
        //make immutable
        for (String roleName : tmpRolesWithRestrictions.keySet()) {
            List<RestrictionDTO> restrictions = tmpRolesWithRestrictions.get(roleName);
            tmpRolesWithRestrictions.put(roleName, Collections.unmodifiableList(restrictions));
        }
        rolesWithRestrictions = Collections.unmodifiableMap(tmpRolesWithRestrictions);
    }

    void resetUserRestrictionList() {
        userRestrictions = new ConcurrentHashMap<>();
    }

    public List<RestrictionEntity> getUserRestrictionsForLoggedInUser() {
        return getUserRestrictions(getCurrentUserName());
    }

    /**
     * Returns a (cached) list of all Restrictions assigned to a specific UserRestriction
     *
     * @param userName
     */
    public List<RestrictionEntity> getUserRestrictions(String userName) {
        if (userRestrictions == null) {
            resetUserRestrictionList();
        }
        if (!userRestrictions.containsKey(userName)) {
            userRestrictions.put(userName, Collections.unmodifiableList(permissionRepository.getUserWithRestrictions(userName)));
        }
        return userRestrictions.get(userName);
    }

    /**
     * Returns a list of all available Restrictions assigned to UserRestriction
     *
     * @return List<RestrictionEntity>
     */
    public List<RestrictionEntity> getAllUserRestrictions() {
        return permissionRepository.getUsersWithRestrictions();
    }

    private void addPermission(Map<String, List<RestrictionDTO>> tmpRolesWithRestrictions, RoleEntity role) {
        String roleName = role.getName();
        if (!tmpRolesWithRestrictions.containsKey(roleName)) {
            tmpRolesWithRestrictions.put(roleName, new ArrayList<RestrictionDTO>());
        }
        for (RestrictionEntity res : role.getRestrictions()) {
            // add restriction
            tmpRolesWithRestrictions.get(roleName).add(new RestrictionDTO(res));
        }
    }

    /**
     * Checks if a user has a role or a restriction with a certain Permission no matter for which Actions
     * Useful for displaying/hiding navigation elements in views
     * The specific Action required has to be checked when the action is involved (button)
     *
     * @param permission
     */
    public boolean hasPermission(Permission permission) {
        return hasPermission(permission, null, ALL, null, null);
    }

    /**
     * Checks if a user has a role or a restriction with a certain Permission and Action
     * Useful for displaying/hiding navigation elements in views
     *
     * @param permission
     */
    public boolean hasPermission(Permission permission, Action action) {
        return hasPermission(permission, null, action, null, null);
    }

    public boolean hasPermission(Permission permission, Action action, ResourceTypeEntity resourceType) {
        return hasPermission(permission, null, action, null, resourceType);
    }

    /**
     * Checks if a user has a role or a restriction with a certain Permission
     *
     * @param permission the required Permission
     * @param context the requested Context (null = irrelevant)
     * @param action the required Action
     * @param resourceGroup the requested resourceGroup (null = irrelevant)
     * @param resourceType the requested resourceType (null = irrelevant)
     */
    public boolean hasPermission(Permission permission, ContextEntity context, Action action,
                                 ResourceGroupEntity resourceGroup, ResourceTypeEntity resourceType) {
        return hasRole(permission.name(), context, action, resourceGroup, resourceType) ||
                hasUserRestriction(permission.name(), context, action, resourceGroup, resourceType);
    }

    /**
     * Checks if given permission is available. If not a exception is created with error message containing extraInfo part.
     *
     * @param permission
     * @param extraInfo
     */
    public void checkPermissionAndFireException(Permission permission, String extraInfo) {
        if (!hasPermission(permission)) {
            throwNotAuthorizedException(extraInfo);
        }
    }

    /**
     * Checks if given permission is available. If not a exception is created with error message containing extraInfo part.
     *
     * @param permission
     * @param extraInfo
     */
    public void checkPermissionAndFireException(Permission permission, Action action, String extraInfo) {
        if (!hasPermission(permission, action)) {
            throwNotAuthorizedException(extraInfo);
        }
    }

    /**
     * Checks if given permission is available. If not a exception is created with error message containing extraInfo part.
     *
     * @param permission
     * @param context
     * @param action
     * @param resourceGroup
     * @param resourceType
     * @param extraInfo
     */
    public void checkPermissionAndFireException(Permission permission, ContextEntity context, Action action,
                                                ResourceGroupEntity resourceGroup, ResourceTypeEntity resourceType,
                                                String extraInfo) {
        if (!hasPermission(permission, context, action, resourceGroup, resourceType)) {
            throwNotAuthorizedException(extraInfo);
        }
    }

    public void throwNotAuthorizedException(String extraInfo) {
        String errorMessage = "Not Authorized!";
        if (StringUtils.isNotEmpty(extraInfo)) {
            errorMessage += " You're not allowed to " + extraInfo + "!";
        }
        throw new NotAuthorizedException(errorMessage);
    }

    private boolean hasRole(String permissionName, ContextEntity context, Action action, ResourceGroupEntity resourceGroup, ResourceTypeEntity resourceType) {
        if (getCurrentUserName() == null) {
            return false;
        }

        for (String roleName : getRolesWithRestrictions().keySet()) {
            for(RestrictionDTO restrictionDTO : getRolesWithRestrictions().get(roleName)) {
                if (matchRestriction(permissionName, action, context, resourceGroup, resourceType, restrictionDTO.getRestriction())) {
                    if(sessionContext.isCallerInRole(roleName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the logged-in user has a Restriction with the required Permission for a specific Context, Action,
     * ResourceGroup and/or ResourceType
     * => will skip context check if context is null
     *
     * @param permissionName
     * @param context
     * @param action
     * @param resourceGroup
     * @param resourceType
     */
    private boolean hasUserRestriction(String permissionName, ContextEntity context, Action action, ResourceGroupEntity resourceGroup, ResourceTypeEntity resourceType) {
        if (getCurrentUserName() == null) {
            return false;
        }

        for (RestrictionEntity restriction : getUserRestrictions(getCurrentUserName())) {
            if (matchRestriction(permissionName, action, context, resourceGroup, resourceType, restriction)) {
                return true;
            }
        }

        return false;
    }
    
    private boolean matchRestriction(String permissionName, Action action, ContextEntity context, ResourceGroupEntity resourceGroup,
            ResourceTypeEntity resourceType, RestrictionEntity restriction) {
        return restriction.getPermission().getValue().equals(permissionName)
                && hasPermissionForAction(restriction, action) 
                && hasPermissionForResourceGroup(restriction, resourceGroup)
                && hasPermissionForResourceType(restriction, resourceType)
                && hasPermissionForDefaultResourceType(restriction, resourceType)
                && hasPermissionForContextOrForParent(restriction, context);
    }

    /**
     * Checks if a Restriction gives permission for a specific Context or its parent
     * No Context on Restriction means all Contexts are allowed
     *
     * @param restriction
     * @param context
     */
    private boolean hasPermissionForContextOrForParent(RestrictionEntity restriction, ContextEntity context) {
        return restriction.getContext() == null ||
                (context != null && (restriction.getContext().getId().equals(context.getId()) ||
                        (context.getParent() != null && restriction.getContext().getId().equals(context.getParent().getId()))));
    }

    /**
     * Checks if a Restriction gives permission for a specific Action
     *
     * @param restriction
     * @param action
     */
    private boolean hasPermissionForAction(RestrictionEntity restriction, Action action) {
        return action == null || restriction.getAction().equals(action) ||
                restriction.getAction().equals(ALL);
    }


    /**
     * Checks if a Restriction gives permission for a specific ResourceGroup
     * No Resource on Restriction means all ResourceGroups are allowed
     *
     * @param restriction
     * @param resourceGroup
     */
    private boolean hasPermissionForResourceGroup(RestrictionEntity restriction, ResourceGroupEntity resourceGroup) {
        if (checkGroup(restriction.getResourceGroup(), resourceGroup)) {
            return true;
        }
        if (checkGroupType(restriction.getResourceType(), resourceGroup.getResourceType())) {
            return true;
        }
        return checkGroupCategory(restriction.getResourceTypePermission(), resourceGroup.getResourceType());

    }

    private boolean checkGroup(ResourceGroupEntity restrictionResourceGroup, ResourceGroupEntity resourceGroup) {
        if (restrictionResourceGroup == null) {
            return false;
        }
        return restrictionResourceGroup.getId().equals(resourceGroup.getId());
    }

    private boolean checkGroupType(ResourceTypeEntity restrictionResourceType, ResourceTypeEntity groupResourceType) {
        if (restrictionResourceType == null) {
            return false;
        }
        if (restrictionResourceType.getId().equals(groupResourceType.getId())) {
            return true;
        }
        // check if resourceType restriction matches resourceGroup parent type
        if (groupResourceType.getParentResourceType() == null) {
            return false;
        }
        return restrictionResourceType.getId().equals(groupResourceType.getParentResourceType().getId());
    }

    private boolean checkGroupCategory(ResourceTypePermission resourceTypeRestriction, ResourceTypeEntity groupResourceType) {
        if (resourceTypeRestriction.equals(ResourceTypePermission.ANY)) {
            return true;
        }
        // Only DefaultTypes are allowed
        if (resourceTypeRestriction.equals(ResourceTypePermission.DEFAULT_ONLY)
                && DefaultResourceTypeDefinition.contains(groupResourceType.getName())) {
            return true;
        }
        // Only non DefaultTypes are allowed
        return resourceTypeRestriction.equals(ResourceTypePermission.NON_DEFAULT_ONLY)
                && !DefaultResourceTypeDefinition.contains(groupResourceType.getName());
    }

    /**
     * Checks if a Restriction gives permission for a specific ResourceType
     * No ResourceType on Restriction means all ResourceTypes are allowed
     *
     * @param restriction
     * @param resourceType
     */
    private boolean hasPermissionForResourceType(RestrictionEntity restriction, ResourceTypeEntity resourceType) {
        if (restriction.getResourceType() == null) {
            return false;
        }
        if (resourceType == null) {
            return false;
        }
        if (restriction.getResourceType().getId().equals(resourceType.getId())) {
            return true;
        }
        return resourceType.getParentResourceType() != null &&
                restriction.getResourceType().getId().equals(resourceType.getParentResourceType().getId());
    }

    /**
     * Check if the user can delete instances of ResourceTypes
     *
     * @param resourceType
     */
    public boolean hasPermissionToRemoveInstanceOfResType(ResourceTypeEntity resourceType) {
        return hasPermission(Permission.RESOURCE, Action.DELETE, resourceType);
    }

    /**
     * Checks if a user is allowed to add a Relation to a Resource
     *
     * @param resourceEntity
     * @param context
     */
    public boolean hasPermissionToAddRelation(ResourceEntity resourceEntity, ContextEntity context) {
        return hasPermission(Permission.RESOURCE, context, Action.UPDATE, resourceEntity.getResourceGroup(), resourceEntity.getResourceType());
    }

    /**
     * Checks if user is allowed to add a Relation to a ResourceType
     *
     * @param resourceTypeEntity
     */
    public boolean hasPermissionToAddRelatedResourceType(ResourceTypeEntity resourceTypeEntity) {
        if (resourceTypeEntity != null) {
            if (hasPermission(Permission.RESOURCETYPE, null, Action.UPDATE, null, resourceTypeEntity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user may delete a Resource relationship
     */
    public boolean hasPermissionToDeleteRelation(ResourceEntity resourceEntity, ContextEntity context) {
        if (resourceEntity != null && resourceEntity.getResourceType() != null) {
            if (hasPermission(Permission.RESOURCE, context, Action.UPDATE, resourceEntity.getResourceGroup(), resourceEntity.getResourceType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user may delete a ResourceType relationship
     */
    public boolean hasPermissionToDeleteRelationType(ResourceTypeEntity resourceTypeEntity) {
        return hasPermission(Permission.RESOURCETYPE, null, Action.UPDATE, null, resourceTypeEntity);
    }

    /**
     * Checks if user may create or edit Templates of Resources
     *
     * @param resource
     * @param isTestingMode
     */
    public boolean hasPermissionToAddResourceTemplate(ResourceEntity resource, boolean isTestingMode) {
        // ok if user has update permission on the Resource, context is always global, so we set it to null to omit the check
        if (hasPermission(Permission.RESOURCE_TEMPLATE, null, Action.CREATE, resource.getResourceGroup(), null)) {
            return true;
        }
        return isTestingMode && hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    public boolean hasPermissionToUpdateResourceTemplate(ResourceEntity resource, boolean isTestingMode) {
        // ok if user has update permission on the Resource, context is always global, so we set it to null to omit the check
        if (hasPermission(Permission.RESOURCE_TEMPLATE, null, Action.UPDATE, resource.getResourceGroup(), null)) {
            return true;
        }
        return isTestingMode && hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    /**
     * Checks if user may create or edit Templates of ResourceTypes
     *
     * @param resourceType
     * @param isTestingMode
     */
    public boolean hasPermissionToAddResourceTypeTemplate(ResourceTypeEntity resourceType, boolean isTestingMode) {
        // ok if user has update permission on the ResourceType, context is always global, so we set it to null to omit the check
        if (hasPermission(Permission.RESOURCETYPE_TEMPLATE, null, Action.CREATE, null, resourceType)) {
            return true;
        }
        return resourceType != null && isTestingMode && hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    public boolean hasPermissionToUpdateResourceTypeTemplate(ResourceTypeEntity resourceType, boolean isTestingMode) {
        // ok if user has update permission on the ResourceType, context is always global, so we set it to null to omit the check
        if (hasPermission(Permission.RESOURCETYPE_TEMPLATE, null, Action.UPDATE, null, resourceType)) {
            return true;
        }
        return resourceType != null && isTestingMode && hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    /**
     * Diese Methode gibt den Username zurück
     */
    public String getCurrentUserName() {
        return sessionContext.getCallerPrincipal() != null ? sessionContext.getCallerPrincipal().getName() : null;
    }

    /**
     * Returns a list of ALL Restrictions of the caller (both, Restrictions by User and Role)
     */
    public List<RestrictionEntity> getAllCallerRestrictions() {
        List<RestrictionEntity> restrictions = new ArrayList<>();
        Map<String, List<RestrictionDTO>> roleWithRestrictions = getRolesWithRestrictions();
        for (String roleName : roleWithRestrictions.keySet()) {
            if (sessionContext.isCallerInRole(roleName)) {
                for (RestrictionDTO restrictionDTO : roleWithRestrictions.get(roleName)) {
                    restrictions.add(restrictionDTO.getRestriction());
                }
            }
        }
        List<RestrictionEntity> userRestrictions = getUserRestrictions(getCurrentUserName());
        restrictions.addAll(userRestrictions);
        return restrictions;
    }

    /**
     * Checks if the caller has the required rights to delegate a specific Restriction to another user
     * The caller must have
     * <li>a PERMISSION_DELEGATION Permission</li>
     * <li>a similar Restriction as the one he wants to delegate</li>
     *
     * @param permission to be delegated
     * @param resourceGroup allowed by the permission to be delegated
     * @param resourceType allowed by the permission to be delegated
     * @param context allowed by the permission to be delegated
     * @param action allowed by the permission to be delegated
     */
    public boolean hasPermissionToDelegatePermission(Permission permission, ResourceGroupEntity resourceGroup,
                                                     ResourceTypeEntity resourceType, ContextEntity context, Action action) {
        if (hasPermission(Permission.PERMISSION_DELEGATION)) {
            return hasPermission(permission, context, action, resourceGroup, resourceType);
        }
        return false;
    }
}
