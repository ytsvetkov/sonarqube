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
package org.sonar.server.computation.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache of the lines of the currently processed file. Only a <strong>single</strong> file
 * is kept in memory at a time. Moreover data is loaded on demand to avoid
 * useless db trips.
 * <br />
 * It assumes that db table FILE_SOURCES is up-to-date before using this
 * cache.
 */
public class SourceLinesCache {

  private final SourceLineIndex index;
  private final BatchReportReader reportReader;

  private boolean loaded = false;
  private BatchReport.Scm scm;
  private String currentFileUuid;
  private Integer currentFileReportRef;

  // date of the latest commit on the file
  private long lastCommitDate = 0L;

  // author of the latest commit on the file
  private String lastCommitAuthor = null;

  public SourceLinesCache(BatchReportReader reportReader, SourceLineIndex index) {
    this.reportReader = reportReader;
    this.index = index;
  }

  /**
   * Marks the currently processed component
   */
  void init(String fileUuid, @Nullable Integer fileReportRef) {
    loaded = false;
    currentFileUuid = fileReportRef == null ? null : fileUuid;
    currentFileReportRef = fileReportRef;
    lastCommitDate = 0L;
    lastCommitAuthor = null;
    clear();
  }

  /**
   * Last committer of the line, can be null.
   * @param lineIndex starts at 0
   */
  @CheckForNull
  public String lineAuthor(@Nullable Integer lineIndex) {
    loadIfNeeded();

    if (lineIndex == null) {
      // issue on file, approximately estimate that author is the last committer on the file
      return lastCommitAuthor;
    }
    String author = null;
    if (lineIndex < scm.getChangesetIndexByLineCount()) {
      BatchReport.Scm.Changeset changeset = scm.getChangeset(scm.getChangesetIndexByLine(lineIndex));
      author = changeset.hasAuthor() ? changeset.getAuthor() : null;
    }

    return StringUtils.defaultIfEmpty(author, lastCommitAuthor);
  }

  /**
   * Load only on demand, to avoid useless db requests on files without any new issues
   */
  private void loadIfNeeded() {
    if (currentFileUuid == null || currentFileReportRef == null) {
      throw new IllegalStateException(String.format("uuid (%s) and report reference (%d) must not be null to use the cache", currentFileUuid, currentFileReportRef));
    }
    if (!loaded) {
      scm = reportReader.readComponentScm(currentFileReportRef);
      loaded = scm != null;
      if (!loaded) {
        List<SourceLineDoc> lines = index.getLines(currentFileUuid);
        Map<String, BatchReport.Scm.Changeset> changesetByRevision = new HashMap<>();
        BatchReport.Scm.Builder scmBuilder = BatchReport.Scm.newBuilder()
          .setComponentRef(currentFileReportRef);
        for (SourceLineDoc sourceLine : lines) {
          if (changesetByRevision.get(sourceLine.scmRevision()) == null) {
            BatchReport.Scm.Changeset changeset = BatchReport.Scm.Changeset.newBuilder()
              .setAuthor(sourceLine.scmAuthor())
              .setDate(sourceLine.scmDate().getTime())
              .setRevision(sourceLine.scmRevision())
              .build();
            scmBuilder.addChangeset(changeset);
            scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetCount() - 1);
            changesetByRevision.put(sourceLine.scmRevision(), changeset);
          } else {
            scmBuilder.addChangesetIndexByLine(scmBuilder.getChangesetList().indexOf(changesetByRevision.get(sourceLine.scmRevision())));
          }
        }
        scm = scmBuilder.build();
        loaded = true;
      }

      for (BatchReport.Scm.Changeset changeset : scm.getChangesetList()) {
        if (changeset.hasAuthor() && changeset.hasDate() && changeset.getDate() > lastCommitDate) {
          lastCommitDate = changeset.getDate();
          lastCommitAuthor = changeset.getAuthor();
        }
      }

    }
  }

  /**
   * Makes cache eligible to GC
   */
  public void clear() {
    scm = null;
  }
}
