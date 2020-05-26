/*
 * This file is generated by jOOQ.
 */
package com.heerkirov.animation.jooq.tables;


import com.heerkirov.animation.jooq.Keys;
import com.heerkirov.animation.jooq.Public;
import com.heerkirov.animation.jooq.tables.records.UserRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class User extends TableImpl<UserRecord> {

    private static final long serialVersionUID = -751185832;

    /**
     * The reference instance of <code>public.user</code>
     */
    public static final User USER = new User();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<UserRecord> getRecordType() {
        return UserRecord.class;
    }

    /**
     * The column <code>public.user.id</code>.
     */
    public final TableField<UserRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('user_id_seq'::regclass)", org.jooq.impl.SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>public.user.username</code>.
     */
    public final TableField<UserRecord, String> USERNAME = createField(DSL.name("username"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>public.user.is_staff</code>.
     */
    public final TableField<UserRecord, Boolean> IS_STAFF = createField(DSL.name("is_staff"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("false", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>public.user.setting</code>.
     */
    public final TableField<UserRecord, JSONB> SETTING = createField(DSL.name("setting"), org.jooq.impl.SQLDataType.JSONB.nullable(false).defaultValue(org.jooq.impl.DSL.field("'{}'::jsonb", org.jooq.impl.SQLDataType.JSONB)), this, "");

    /**
     * Create a <code>public.user</code> table reference
     */
    public User() {
        this(DSL.name("user"), null);
    }

    /**
     * Create an aliased <code>public.user</code> table reference
     */
    public User(String alias) {
        this(DSL.name(alias), USER);
    }

    /**
     * Create an aliased <code>public.user</code> table reference
     */
    public User(Name alias) {
        this(alias, USER);
    }

    private User(Name alias, Table<UserRecord> aliased) {
        this(alias, aliased, null);
    }

    private User(Name alias, Table<UserRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> User(Table<O> child, ForeignKey<O, UserRecord> key) {
        super(child, key, USER);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public Identity<UserRecord, Integer> getIdentity() {
        return Keys.IDENTITY_USER;
    }

    @Override
    public UniqueKey<UserRecord> getPrimaryKey() {
        return Keys.USER_PKEY;
    }

    @Override
    public List<UniqueKey<UserRecord>> getKeys() {
        return Arrays.<UniqueKey<UserRecord>>asList(Keys.USER_PKEY);
    }

    @Override
    public User as(String alias) {
        return new User(DSL.name(alias), this);
    }

    @Override
    public User as(Name alias) {
        return new User(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public User rename(String name) {
        return new User(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public User rename(Name name) {
        return new User(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, Boolean, JSONB> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
