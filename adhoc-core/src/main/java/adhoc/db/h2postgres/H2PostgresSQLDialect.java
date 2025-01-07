/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package adhoc.db.h2postgres;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.dialect.PostgreSQLSqlAstTranslator;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;

/** Dialect to avoid use of <tt>for no key update</tt> as it is not available in H2's Postgres emulation. */
public class H2PostgresSQLDialect extends PostgreSQLDialect {

    public static final DatabaseVersion PRETEND_POSTGRES_VERSION = DatabaseVersion.make(17, 0, 0);

    public H2PostgresSQLDialect() {
        super();
    }

    public H2PostgresSQLDialect(DialectResolutionInfo dialectResolutionInfo) {
        super(PRETEND_POSTGRES_VERSION);
    }

    public H2PostgresSQLDialect(DatabaseVersion databaseVersion) {
        super(PRETEND_POSTGRES_VERSION);
    }

    public H2PostgresSQLDialect(DatabaseVersion databaseVersion, PostgreSQLDriverKind postgreSQLDriverKind) {
        super(PRETEND_POSTGRES_VERSION, postgreSQLDriverKind);
    }

    @Override
    public H2PostgresSqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new H2PostgresSqlAstTranslatorFactory();
    }

    private static class H2PostgresSqlAstTranslatorFactory extends StandardSqlAstTranslatorFactory {

        @Override
        protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
            return new H2PostgresSQLSqlAstTranslator<>(sessionFactory, statement);
        }
    }

    private static class H2PostgresSQLSqlAstTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {

        H2PostgresSQLSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
            super(sessionFactory, statement);
        }

        @Override
        protected String getForUpdate() {
            return " for update";
        }
    }
}
