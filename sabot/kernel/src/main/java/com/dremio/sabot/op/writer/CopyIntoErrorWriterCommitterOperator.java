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
package com.dremio.sabot.op.writer;

import com.dremio.common.exceptions.ExecutionSetupException;
import com.dremio.exec.physical.config.copyinto.CopyIntoErrorWriterCommitterPOP;
import com.dremio.sabot.exec.context.OperatorContext;
import com.dremio.sabot.op.spi.SingleInputOperator;

/**
 * A custom implementation of the WriterCommitterOperator for handling errors in the copy-into operation.
 */
public class CopyIntoErrorWriterCommitterOperator extends WriterCommitterOperator {
  public CopyIntoErrorWriterCommitterOperator(OperatorContext context, CopyIntoErrorWriterCommitterPOP config) {
    super(context, config);
  }

  /**
   * A custom implementation of SingleInputOperator.Creator for creating instances of CopyIntoErrorWriterCommitterOperator.
   */
  public static class WriterCreator implements SingleInputOperator.Creator<CopyIntoErrorWriterCommitterPOP> {

    @Override
    public SingleInputOperator create(OperatorContext context, CopyIntoErrorWriterCommitterPOP operator) throws ExecutionSetupException {
      return new CopyIntoErrorWriterCommitterOperator(context, operator);
    }
  }
}
