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

package ch.puzzle.itc.mobiliar.business.usersettings.entity;

import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "TAMW_favoriteResource", uniqueConstraints = {
        @UniqueConstraint(name = "FAV_UNIQUE", columnNames = {"resourcegroup_id", "user_id"})})
public class FavoriteResourceEntity implements Serializable {


    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    @TableGenerator(name = "favoriteResourceIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "favoriteResourceId")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "favoriteResourceIdGen")
    @Id
    @Column(unique = true, nullable = false)
    private Integer id;

    @Getter
    @Setter
    @Column(nullable = false)
    private boolean email;

    @Getter
    @Setter
    @ManyToOne
    @org.hibernate.annotations.ForeignKey(name = "FK_BP9VL4DQWBSN2ECTPJ4DEF8V7")
    private UserSettingsEntity user;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn
    @org.hibernate.annotations.ForeignKey(name = "FAVGROUP_FK")
    private ResourceGroupEntity resourceGroup;

    @Getter
    @Version
    private long v;

}
