(defproject frontier "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"sonatype" "https://oss.sonatype.org/content/groups/public/"
                 "my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.namespace "0.2.7"]
                 [com.datastax.cassandra/cassandra-driver-core "2.1.0"]
                 [com.datomic/datomic-pro "0.9.4899"
                  :exclusions [org.slf4j/slf4j-nop
                               org.slf4j/slf4j-log4j12
                               org.slf4j/jul-to-slf4j
                               org.slf4j/log4j-over-slf4j]]
                 [io.netty/netty-all "5.0.0.Alpha2-SNAPSHOT"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"
                  :exclusions [org.clojure/tools.namespace]]
                 [com.googlecode.lanterna/lanterna "2.1.9"]
                 [com.taoensso/carmine "2.7.0"
                  :exclusions [org.clojure/clojure]]
                 [crypto-password "0.1.3"]
                 [org.slf4j/slf4j-api "1.6.2"]
                 [org.slf4j/slf4j-log4j12 "1.6.2"]
                 [log4j "1.2.16"]
                 [commons-logging "1.1.1"]]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]]
                   :source-paths ["dev"]}})
