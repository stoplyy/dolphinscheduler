-- Active: 1714116867812@@127.0.0.1@5432@dolphinscheduler
DROP TABLE IF EXISTS t_ds_project_cluster;

CREATE TABLE t_ds_project_cluster (
    id SERIAL PRIMARY KEY,
    project_code BIGINT NOT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_name TEXT DEFAULT NULL,
    data_from TEXT DEFAULT NULL,
    appid TEXT DEFAULT NULL,
    user_id INTEGER DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_node;

CREATE TABLE t_ds_project_node (
    id SERIAL PRIMARY KEY,
    user_id INTEGER DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_code INTEGER DEFAULT NULL,
    data_source_code BIGINT DEFAULT NULL,
    node_id TEXT DEFAULT NULL,
    node_key TEXT DEFAULT NULL,
    node_name TEXT DEFAULT NULL,
    data_from TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_node_parameter;

CREATE TABLE t_ds_project_node_parameter (
    id SERIAL PRIMARY KEY,
    user_id INTEGER DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    node_id TEXT DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_code INTEGER DEFAULT NULL,
    node_code INTEGER DEFAULT NULL,
    param_name TEXT DEFAULT NULL,
    param_value TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_cluster_parameter;

CREATE TABLE t_ds_project_cluster_parameter (
    id SERIAL PRIMARY KEY,
    user_id INTEGER DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_name TEXT DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_code INTEGER DEFAULT NULL,
    param_name TEXT DEFAULT NULL,
    param_value TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);