(defproject gobanbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.14.4"]
                 [clj-http "3.9.1"]
                 [metosin/compojure-api "1.1.12"]
                 [ring-logger "1.0.1"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [org.clojure/tools.logging "0.4.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                 javax.jms/jms
                                                 com.sun.jmdk/jmxtools
                                                 com.sun.jmx/jmxri]]
                 [morse "0.4.0"]
                 [dali "0.7.4"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.xerial/sqlite-jdbc "3.23.1"]]
  :main gobanbot.web
  :aot [gobanbot.web]
  :uberjar-name "gobanbot-standalone.jar"
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler gobanbot.web/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
                   :uberjar {:aot :all}})
