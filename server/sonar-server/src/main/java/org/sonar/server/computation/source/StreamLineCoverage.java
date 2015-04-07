/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.source;

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import javax.annotation.CheckForNull;

import java.util.Iterator;

public class StreamLineCoverage implements StreamLine {

  private final Iterator<BatchReport.Coverage> coverageIterator;
  private BatchReport.Coverage coverage;

  public StreamLineCoverage(Iterator<BatchReport.Coverage> coverageIterator) {
    this.coverageIterator = coverageIterator;
    coverage = null;
  }

  @Override
  public void readLine(int line, FileSourceDb.Line.Builder lineBuilder) {
    BatchReport.Coverage reportCoverage = getNextReport(line);
    if (reportCoverage != null) {
      processUnitTest(lineBuilder, reportCoverage);
      processIntegrationTest(lineBuilder, reportCoverage);
      processOverallTest(lineBuilder, reportCoverage);

      // Reset coverage
      coverage = null;
    }
  }

  private static void processUnitTest(FileSourceDb.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage){
    if (hasUtLineHits(reportCoverage)) {
      lineBuilder.setUtLineHits(1);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasUtCoveredConditions()) {
      lineBuilder.setUtConditions(reportCoverage.getConditions());
      lineBuilder.setUtCoveredConditions(reportCoverage.getUtCoveredConditions());
    }
  }

  private static boolean hasUtLineHits(BatchReport.Coverage reportCoverage){
    return reportCoverage.hasUtHits() && reportCoverage.getUtHits();
  }

  private static void processIntegrationTest(FileSourceDb.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage){
    if (hasItLineHits(reportCoverage)) {
      lineBuilder.setItLineHits(1);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasItCoveredConditions()) {
      lineBuilder.setItConditions(reportCoverage.getConditions());
      lineBuilder.setItCoveredConditions(reportCoverage.getItCoveredConditions());
    }
  }

  private static boolean hasItLineHits(BatchReport.Coverage reportCoverage){
    return reportCoverage.hasItHits() && reportCoverage.getItHits();
  }

  private static void processOverallTest(FileSourceDb.Line.Builder lineBuilder, BatchReport.Coverage reportCoverage){
    if (hasUtLineHits(reportCoverage) || hasItLineHits(reportCoverage)) {
      lineBuilder.setOverallLineHits(1);
    }
    if (reportCoverage.hasConditions() && reportCoverage.hasOverallCoveredConditions()) {
      lineBuilder.setOverallConditions(reportCoverage.getConditions());
      lineBuilder.setOverallCoveredConditions(reportCoverage.getOverallCoveredConditions());
    }
  }

  @CheckForNull
  private BatchReport.Coverage getNextReport(int currentLine) {
    // Get next element (if exists)
    if (coverage == null && coverageIterator.hasNext()) {
      coverage = coverageIterator.next();
    }
    // Return current element if lines match
    if (coverage != null && coverage.getLine() == currentLine) {
      return coverage;
    }
    return null;
  }

}
