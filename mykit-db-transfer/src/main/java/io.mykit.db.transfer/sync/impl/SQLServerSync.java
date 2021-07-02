/**
 * Copyright 2018-2118 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mykit.db.transfer.sync.impl;

import io.mykit.db.common.constants.MykitDbSyncConstants;
import io.mykit.db.common.utils.StringUtils;
import io.mykit.db.transfer.entity.JobInfo;
import io.mykit.db.transfer.sync.DBSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;

/**
 * @author binghe
 * @description SQL Server数据库同步实现
 * @version 1.0.0
 */
public class SQLServerSync extends AbstractDBSync implements DBSync {
    private Logger logger = LoggerFactory.getLogger(SQLServerSync.class);

    @Override
    public String assembleSQL(String srcSql, Connection conn, JobInfo jobInfo) throws SQLException {
        String fieldStr = jobInfo.getDestTableFields();
        String[] destFields = jobInfo.getDestTableFields().split(",");
        destFields = this.trimArrayItem(destFields);

        //默认的srcFields数组与destFields相同
        String[] srcFields = destFields;
        String srcField = jobInfo.getSrcTableFields();
        if(!StringUtils.isEmpty(srcField)){
            srcFields = this.trimArrayItem(srcField.split(MykitDbSyncConstants.FIELD_SPLIT));
        }
        Map<String, String> fieldMapper = this.getFieldsMapper(srcFields, destFields);

        String[] updateFields = jobInfo.getDestTableUpdate().split(",");
        updateFields = this.trimArrayItem(updateFields);
        String destTableKey = jobInfo.getDestTableKey();
        String destTable = jobInfo.getDestTable();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery(srcSql);
        StringBuffer sql = new StringBuffer();
        long count = 0;
        while (rs.next()) {
            sql.append("if not exists (select ").append(destTableKey).append(" from ").append(destTable).append(" where ").append(destTableKey).append("='").append(rs.getString(destTableKey))
                    .append("')").append("insert into ").append(destTable).append("(").append(fieldStr).append(") values(");
            for (int index = 0; index < destFields.length; index++) {
                sql.append("'").append(rs.getString(fieldMapper.get(destFields[index]).trim())).append(index == (destFields.length - 1) ? "'" : "',");
            }
            sql.append(") else update ").append(destTable).append(" set ");
            for (int index = 1; index < updateFields.length; index++) {
                sql.append(updateFields[index]).append("='").append(rs.getString(fieldMapper.get(destFields[index]).trim())).append(index == (updateFields.length - 1) ? "'" : "',");
            }
            sql.append(" where ").append(destTableKey).append("='").append(rs.getString(destTableKey)).append("';");
            count++;
            // this.logger.info("第" + count + "耗时: " + (new Date().getTime() - oneStart) + "ms");
        }
        this.logger.info("总共查询到 " + count + " 条记录");
        if (rs != null) {
            rs.close();
        }
        if (stat != null) {
            stat.close();
        }
        return count > 0 ? sql.toString() : null;
    }

    @Override
    public void executeSQL(String sql, Connection conn) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.executeUpdate();
        conn.commit();
        pst.close();
    }
}
