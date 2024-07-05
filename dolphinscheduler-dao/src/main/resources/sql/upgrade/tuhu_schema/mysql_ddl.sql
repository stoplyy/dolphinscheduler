DROP TABLE IF EXISTS t_ds_project_cluster;

CREATE TABLE t_ds_project_cluster (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_code BIGINT NOT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_name TEXT DEFAULT NULL,
    data_from TEXT DEFAULT NULL,
    appid TEXT DEFAULT NULL,
    user_id INT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_node;

CREATE TABLE t_ds_project_node (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_code INT DEFAULT NULL,
    data_source_code BIGINT DEFAULT NULL,
    node_id TEXT DEFAULT NULL,
    node_key TEXT DEFAULT NULL,
    node_name TEXT DEFAULT NULL,
    data_from TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_node_parameter;

CREATE TABLE t_ds_project_node_parameter (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    node_id TEXT DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_code INT DEFAULT NULL,
    node_code INT DEFAULT NULL,
    param_name TEXT DEFAULT NULL,
    param_value TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS t_ds_project_cluster_parameter;

CREATE TABLE t_ds_project_cluster_parameter (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT NULL,
    cluster_id TEXT DEFAULT NULL,
    cluster_name TEXT DEFAULT NULL,
    project_code BIGINT DEFAULT NULL,
    cluster_code INT DEFAULT NULL,
    param_name TEXT DEFAULT NULL,
    param_value TEXT DEFAULT NULL,
    des TEXT DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);