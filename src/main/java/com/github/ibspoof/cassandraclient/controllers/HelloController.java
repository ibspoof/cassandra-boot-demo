package com.github.ibspoof.cassandraclient.controllers;

import com.datastax.driver.core.Row;
import com.github.ibspoof.cassandraclient.cassandra.CassandraSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloController {

    @Autowired
    CassandraSession cassandraSession;

    @RequestMapping("/")
    public String index() {

        List<Row> rows = cassandraSession.getSession().execute("select * from examples.purchases").all();

        StringBuilder sb = new StringBuilder();

        for (Row row : rows) {
            sb.append(row.getPartitionKeyToken());
        }

        return sb.toString();
    }

}