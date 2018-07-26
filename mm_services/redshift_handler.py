import psycopg2
import os
import pickle


def redshift_connection():
    envs = {'host': os.environ.get('REDSHIFT_URL'),
            'user': os.environ.get('REDSHIFT_USER'),
            'password': os.environ.get('REDSHIFT_PASSWORD'),
            'port': os.environ.get('REDSHIFT_PORT'),
            'database': os.environ.get('REDSHIFT_DATABASE')}
    missing_envs = [env for env, val in envs.items() if not val]
    if len(missing_envs) > 0:
        for env in missing_envs:
            print("Error: Make sure you have set the env value for " + env)

        exit(4)

    return psycopg2.connect(**envs)


def run_file_queries(files, use_cache=False, *varg, **darg):
    def read_files(_files):
        for file in _files:
            yield open(file, 'r').read()
    os.makedirs('.cache', exist_ok=True)
    cache_file = '.cache/query_cache_' + '-'.join(files).replace('/', '_')
    if use_cache and os.path.isfile(cache_file):
        return pickle.load(open(cache_file, 'rb'))
    result = run_queries(read_files(files), use_cache, *varg, **darg)
    pickle.dump(result, open(cache_file, 'wb'))
    return result


# This method will help us run multiple queries at the same time with a single
# call to this method
# @param queries - an array, glob object or string that points to the sql files
#                  to be run
# @param use_cache - a way to cache the response to speed upp the execution
#                    process
# @param use_same_connection - if the sql files are small we can aggree that it
#                              is faster to use the same postgresql
# connection
# @param use_associative_dicts - Determines how the response will be formated,
#                                if true the response have a dictionary
# with column names and their associative values
def run_queries(queries,
                use_cache=False,
                connection=None,
                use_same_connection=True,
                use_associative_dicts=True,
                some_file_params=None,
                verbose=False):
    if use_same_connection and connection is None:
        conn = redshift_connection()
    if connection:
        conn = connection
    result = []
    os.makedirs('.cache', exist_ok=True)
    for file_content in queries:
        cf = '.cache/query_cache_' + hash(file_content).replace('/', '_')
        if use_cache and os.path.isfile(cf):
            result.append(pickle.load(open(cf, 'rb')))
            continue
        cur = conn.cursor()
        if some_file_params is not None:
            file_content = file_content % some_file_params
        if verbose:
            print(file_content)
        cur.execute(file_content)
        columns = [d[0] for d in cur.description]
        table = []
        for r in cur.fetchall():
            if use_associative_dicts:
                table.append({c: r[i] for i, c in enumerate(columns)})
            else:
                table.append(r)
        result.append(table)
        cur.close()
        pickle.dump(table, open(cf, 'wb'))
    if use_same_connection and connection is None:
        conn.close()
    return result


def run_query(query, *vargs, **dargs):
    return run_queries([query], *vargs, **dargs)[0]


def run_file_query(file, *vargs, **dargs):
    return run_file_queries([file], *vargs, **dargs)[0]


def insert_into(table_name, columns, data, chunkSize=0):
    if chunkSize <= 0:
        chunkSize = 1000  # int(min(1000, len(data) / 10))
    for i in range(0, len(data), chunkSize):
        conn = redshift_connection()
        redshift_cur = conn.cursor()

        data_chunk = data[i:i + chunkSize]
        data_tuple = [tuple(d[c] if isinstance(d[c], str)
                      else float(d[c])
                      for c in columns)
                      for d in data_chunk]
        records_list_template = ','.join(['%s'] * len(data_tuple))
        column_string = ','.join(columns)
        insert_query = 'insert into ' + table_name
        insert_query += ' (' + column_string + ') values {0}'.format(
                        records_list_template)
        redshift_cur.execute(insert_query, data_tuple)

        conn.commit()
        redshift_cur.close()
        conn.close()
