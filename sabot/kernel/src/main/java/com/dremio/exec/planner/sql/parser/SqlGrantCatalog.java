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
package com.dremio.exec.planner.sql.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.ops.QueryContext;
import com.dremio.exec.planner.sql.handlers.direct.SimpleDirectHandler;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

/**
 * Implements SQL Grants for Catalogs.
 */
public class SqlGrantCatalog extends SqlCall implements SimpleDirectHandler.Creator {
  private final SqlNodeList privilegeList;
  private final SqlLiteral grantType;
  private final SqlIdentifier entity;
  private final SqlIdentifier grantee;
  private final SqlLiteral granteeType;

  public enum Privilege {
    USAGE,
    MANAGE_GRANTS,
    CREATE_BRANCH,
    CREATE_TAG,
    MODIFY,
    COMMIT,
    ALL
  }

  public enum GranteeType {
    USER,
    ROLE
  }

  public enum GrantType {
    CATALOG
  }

  public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("GRANT", SqlKind.OTHER) {
    @Override
    public SqlCall createCall(SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
      Preconditions.checkArgument(operands.length == 5, "SqlGrantCatalog.createCall() has to get 5 operands!");
      return new SqlGrantCatalog(
        pos,
        (SqlNodeList) operands[0],
        (SqlLiteral) operands[1],
        (SqlIdentifier) operands[2],
        (SqlLiteral) operands[3],
        (SqlIdentifier) operands[4]
      );
    }
  };

  public SqlGrantCatalog(SqlParserPos pos, SqlNodeList privilegeList, SqlLiteral grantType, SqlIdentifier entity,
                         SqlLiteral granteeType, SqlIdentifier grantee) {
    super(pos);
    this.privilegeList = privilegeList;
    this.grantType = grantType;
    this.entity = entity;
    this.grantee = grantee;
    this.granteeType = granteeType;
  }

  @Override
  public SimpleDirectHandler toDirectHandler(QueryContext context) {
    try {
      final Class<?> cl = Class.forName("com.dremio.exec.planner.sql.handlers.GrantCatalogHandler");
      final Constructor<?> ctor = cl.getConstructor(QueryContext.class);
      return (SimpleDirectHandler) ctor.newInstance(context);
    } catch (ClassNotFoundException e) {
      SqlGrantCatalog.GrantType grantTypeObject = (SqlGrantCatalog.GrantType) (grantType.getValue());
      throw UserException.unsupportedError(e)
        .message("GRANT on %s is not supported in this edition.", grantTypeObject.name())
        .buildSilently();
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("GRANT");
    privilegeList.unparse(writer, leftPrec, rightPrec);
    writer.keyword("ON");
    grantType.unparse(writer, 0, 0);
    writer.keyword("TO");
    grantee.unparse(writer, leftPrec, rightPrec);
  }


  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> ops = Lists.newArrayList();
    ops.add(privilegeList);
    ops.add(grantType);
    ops.add(entity);
    ops.add(grantee);
    ops.add(granteeType);
    return ops;
  }

  public SqlNodeList getPrivilegeList() {
    return privilegeList;
  }

  public SqlLiteral getGrantType() {
    return grantType;
  }

  public SqlIdentifier getEntity() {
    return entity;
  }

  public SqlIdentifier getGrantee() {
    return grantee;
  }

  public SqlLiteral getGranteeType() {
    return granteeType;
  }

  public static class Grant {
    private final SqlLiteral type;

    public Grant(SqlLiteral type) {
      this.type = type;
    }

    public SqlLiteral getType() {
      return type;
    }
  }

}
