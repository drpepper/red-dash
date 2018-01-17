(defproject red-dash "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [http-kit "2.2.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler red-dash.handler/app}
  :jvm-opts ["--add-modules" "java.xml.bind"]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
