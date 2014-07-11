#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

#
# SonarQube 4.4
# SONAR-5218
#
class ConvertProfileMeasures < ActiveRecord::Migration

  class Metric < ActiveRecord::Base
  end

  def self.up
    execute_java_migration('org.sonar.server.db.migrations.v44.ConvertProfileMeasuresMigration')

    Metric.reset_column_information
    metric = Metric.find_by_name('profile')
    if metric
      metric.name='quality_profiles'
      metric.save!
    end
  end

end