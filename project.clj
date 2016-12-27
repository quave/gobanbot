(defproject gobanbot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.11.0"]
                 [clj-http "2.0.0"]
                 [metosin/compojure-api "1.0.0"]
                 [ring-logger "0.7.6"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [morse "0.2.1"]
                 [dali "0.7.3"]
                 [ring/ring-defaults "0.1.2"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]]
  :main gobanbot.web
  :aot [gobanbot.web]
  :uberjar-name "gobanbot-standalone.jar"
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler gobanbot.web/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
                   :uberjar {:aot :all}})
