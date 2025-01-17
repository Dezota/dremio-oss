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
package com.dremio.exec.planner.serializer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.calcite.sql.SqlOperandCountRange;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dremio.plan.serialization.PSqlOperator;
import com.google.common.base.Preconditions;

/**
 * Serialize TO <> FROM SqlOperator to Protobuf.
 */
public final class SqlOperatorSerde {
  private static final Logger logger = LoggerFactory.getLogger(SqlOperatorSerde.class);

  private final SqlOperatorTable sqlOperatorTable;
  private final LegacySqlOperatorSerde legacySqlOperatorSerde;

  public SqlOperatorSerde(SqlOperatorTable sqlOperatorTable) {
    this.sqlOperatorTable = Preconditions.checkNotNull(sqlOperatorTable);
    this.legacySqlOperatorSerde = new LegacySqlOperatorSerde(sqlOperatorTable);
  }

  public SqlOperator fromProto(PSqlOperator o) {
    try {
      // We first try to route the call to the legacy serde for backwards compatablity:
      return legacySqlOperatorSerde.fromProto(o);
    } catch (Exception ex) {
      logger.warn("Legacy Operator Serde Failed for: " + o.getName());
    }

    // These are the cases that the legacy serde could not handle:
    String name;
    switch (o.getSqlOperatorTypeCase()) {
      case NAME:
        name = o.getName();
        break;

      case DNAME:
        name = o.getDname();
        break;

      default:
        throw new UnsupportedOperationException("Unknown name type: " + o.getSqlOperatorTypeCase());
    }

    List<SqlOperator> overloads = sqlOperatorTable
      .getOperatorList()
      .stream()
      .filter(x -> x.getName().equalsIgnoreCase(name))
      .collect(Collectors.toList());

    if (overloads.size() > 1) {
      // Continue to filter by the operand count:
      List<SqlOperator> filteredOverloads = overloads
        .stream()
        .filter(x -> {
          try {
            SqlOperandCountRange sqlOperandCountRange = x.getOperandCountRange();
            return sqlOperandCountRange.getMax() == o.getMaxOperands() && sqlOperandCountRange.getMin() == o.getMinOperands();
          } catch (Exception ex) {
            // Some operators don't implement getOperandCountRange();
            return true;
          }
        })
        .collect(Collectors.toList());
      if (!filteredOverloads.isEmpty()) {
        overloads = filteredOverloads;
      }
    }

    if (overloads.size() > 1) {
      // Then filter by the class name
      List<SqlOperator> filteredOverloads = overloads
        .stream()
        .filter(x -> x.getClass().getName().equalsIgnoreCase(o.getClassName()))
        .collect(Collectors.toList());
      if (!filteredOverloads.isEmpty()) {
        overloads = filteredOverloads;
      }
    }

    if (overloads.isEmpty()) {
      throw new UnsupportedOperationException("Failed to match SQL Operators for: " + o.getName());
    } else if (overloads.size() > 1) {
      // We need an elegant tie breaking algorithm, but for now let's just return the first one:
      return overloads.get(0);
    } else {
      return overloads.get(0);
    }
  }

  public PSqlOperator toProto(SqlOperator o) {
    try {
      // We first try to route the call to the legacy serde for backwards compatablity:
      return legacySqlOperatorSerde.toProto(o);
    } catch (Exception ex) {
      logger.warn("Legacy Operator Serde Failed for: " + o.getName());
    }

    // These are the cases that the legacy serde could not handle:
    PSqlOperator.Builder builder = PSqlOperator
      .newBuilder()
      .setName(o.getName())
      .setClassName(o.getClass().getName());

    try {
      builder
        .setMinOperands(o.getOperandCountRange().getMin())
        .setMaxOperands(o.getOperandCountRange().getMax());
    } catch (Exception ex) {
      // Some methods don't implement getOperandCountRange()
    }

    return builder.build();
  }
}
