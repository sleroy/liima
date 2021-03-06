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

package ch.puzzle.itc.mobiliar.business.globalfunction.boundary;

import ch.puzzle.itc.mobiliar.business.database.entity.MyRevisionEntity;
import ch.puzzle.itc.mobiliar.business.globalfunction.control.GlobalFunctionService;
import ch.puzzle.itc.mobiliar.business.globalfunction.entity.GlobalFunctionEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.business.template.control.FreemarkerSyntaxValidator;
import ch.puzzle.itc.mobiliar.business.template.entity.RevisionInformation;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class GlobalFunctionsBoundary {

    @Inject
    EntityManager entityManager;

    @Inject
    GlobalFunctionService functionService;

    @Inject
    FreemarkerSyntaxValidator freemarkerValidator;

    public List<GlobalFunctionEntity> getAllGlobalFunctions() {
        return functionService.getAllGlobalFunctions();
    }

    public List<GlobalFunctionEntity> getAllGlobalFunctions(Date date) {
        return functionService.getAllGlobalFunctionsAtDate(date);
    }

    public GlobalFunctionEntity getFunctionById(Integer globalFunctionId) {
        return entityManager.find(GlobalFunctionEntity.class, globalFunctionId);
    }

    /**
     * Returns a AmwFunctionEntity identified by its id and revision id
     */
    public GlobalFunctionEntity getFunctionByIdAndRevision(Integer functionId, Number revisionId) {
        GlobalFunctionEntity globalFunctionEntity = AuditReaderFactory.get(entityManager).find(
                GlobalFunctionEntity.class, functionId, revisionId);
        return globalFunctionEntity;
    }

    /**
     * Returns all RevisionInformation for the specified function id
     */
    public List<RevisionInformation> getFunctionRevisions(Integer functionId) {
        List<RevisionInformation> result = new ArrayList<RevisionInformation>();
        if (functionId != null) {
            AuditReader reader = AuditReaderFactory.get(entityManager);
            List<Number> list = reader.getRevisions(GlobalFunctionEntity.class, functionId);
            for (Number rev : list) {
                Date date = reader.getRevisionDate(rev);
                MyRevisionEntity myRev = entityManager.find(MyRevisionEntity.class, rev);
                result.add(new RevisionInformation(rev, date, myRev.getUsername()));
            }
            Collections.sort(result);
        }
        return result;
    }

    @HasPermission(permission = Permission.MANAGE_GLOBAL_FUNCTIONS)
    public void deleteGlobalFunction(Integer globalFunctionId) {
        GlobalFunctionEntity gf = entityManager.find(GlobalFunctionEntity.class, globalFunctionId);
        deleteGlobalFunction(gf);
    }

    @HasPermission(permission = Permission.MANAGE_GLOBAL_FUNCTIONS)
    public void deleteGlobalFunction(GlobalFunctionEntity function) {
        functionService.deleteFunction(function);
    }

    @HasPermission(permission = Permission.MANAGE_GLOBAL_FUNCTIONS)
    public boolean saveGlobalFunction(GlobalFunctionEntity function) throws AMWException {
        if (function != null) {
            if (StringUtils.isEmpty(function.getName())) {
                throw new AMWException("The function name must not be empty");
            }
            freemarkerValidator.validateFreemarkerSyntax(function.getContent());
            return functionService.saveFunction(function);
        }
        return true;
    }
}
