/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.connector.metadata.options;

/**
 * Metadata verify request for verifying if operations between "begin-snapshot" and "end-snapshot" were append-only.
 */
public final class VerifyAppendOnlyRequest implements MetadataVerifyRequest {
  private final String beginSnapshotId;
  private final String endSnapshotId;

  public VerifyAppendOnlyRequest(String beginSnapshotId, String endSnapshotId) {
    this.beginSnapshotId = beginSnapshotId;
    this.endSnapshotId = endSnapshotId;
  }

  public String getBeginSnapshotId() {
    return beginSnapshotId;
  }

  public String getEndSnapshotId() {
    return endSnapshotId;
  }
}
