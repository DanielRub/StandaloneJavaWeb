/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.danielrub.standalonejavaweb;

import com.wix.mysql.config.MysqldConfig;
import com.wix.mysql.EmbeddedMysql;

import java.util.concurrent.TimeUnit;

import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import com.wix.mysql.ScriptResolver;
import static com.wix.mysql.distribution.Version.v5_6_23;
import static com.wix.mysql.config.Charset.UTF8;
import com.wix.mysql.config.DownloadConfig;
import static com.wix.mysql.config.DownloadConfig.aDownloadConfig;
import com.wix.mysql.distribution.Version;

/**
 *
 * @author daniel
 */
public class Start {

    public static void main(String[] args) {
        MysqldConfig config = aMysqldConfig(Version.v5_7_latest)
                .withCharset(UTF8)
                .withPort(12)
                .withUser("sjw", "sjw") //root user is reserved for system use
                .withTimeZone("Afrique/Kinshasa")
                .withTimeout(2, TimeUnit.MINUTES)
                .withServerVariable("max_connect_errors", 666)
                .build();
        EmbeddedMysql mysqld = anEmbeddedMysql(config)
                //.addSchema("aschema", ScriptResolver.classPathScript("db/001_init.sql"))
               // .addSchema("aschema2", ScriptResolver.classPathScripts("db/*.sql"))
                .start();

    }

}
