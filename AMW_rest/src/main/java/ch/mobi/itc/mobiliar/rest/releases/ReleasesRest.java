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

package ch.mobi.itc.mobiliar.rest.releases;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;;

@RequestScoped
@Path("/releases")
@Api(value = "/releases", description = "Releases")
public class ReleasesRest {

    @Inject
    ReleaseMgmtService releaseMgmtService;

    @GET
    @ApiOperation(value = "Get releases", notes = "Returns all releases")
    public List<ReleaseEntity> getReleases() {
        return releaseMgmtService.loadAllReleases(true);
    }

    @GET
    @Path("/{releaseName}")
    @ApiOperation(value = "Get a release", notes = "Returns the specifed release")
    public Response getRelease(@PathParam("releaseName") String releaseName) {
        ReleaseEntity result = releaseMgmtService.findByName(releaseName);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(result).build();
    }
}
