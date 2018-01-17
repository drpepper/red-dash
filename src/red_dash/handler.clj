(ns red-dash.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [hiccup.core :refer :all]
            [hiccup.page :refer :all]))

(def refresh-period 5) ; in seconds


(defn check-redmetrics-server [url]
  (let [{:keys [status body error]} @(http/get url)]
    (if error
      "ERROR contacting host"
      (try 
        (let [data (json/read-str body)] 
          (if (= "1" (get data "apiVersion"))
            :ok
            "ERROR RedMetrics not returning valid data"))
       (catch Exception e "ERROR parsing JSON")))))

(defn check-redwire-server [base-url]
  (let [url (str base-url "/api/games?%7B%22id%22%3A%22count%22%7D")
    {:keys [status body error]} @(http/get url)]
    (if error
      "ERROR contacting host"
      (try 
        (let [data (json/read-str body)] 
          (if (contains? data "count")
            :ok
            "ERROR RedWire not returning valid data"))
       (catch Exception e "ERROR parsing JSON")))))

(defn use-hiccup [handler]
  (fn [request] (-> (handler request) 
    html
    response/response
    (response/content-type "text/html"))))

(defn server-block [redmetrics-url redwire-url]
  (let [redmetrics-status (check-redmetrics-server redmetrics-url)
    redwire-status (check-redwire-server redwire-url)]
    [:div
      [:h3 {:class (if (= :ok redmetrics-status) "ok" "error")} 
        (str "RedMetrics @ " redmetrics-url ":  ") (if (= :ok redmetrics-status) "OK" redmetrics-status)]
      [:h3 {:class (if (= :ok redwire-status) "ok" "error")} 
        (str "RedWire @ " redwire-url ":  ") (if (= :ok redwire-status) "OK" redwire-status)]]))

(defn main-page [request]
  (html5 
    [:head 
      (include-css "styles.css")
      [:meta {"http-equiv" "refresh" 
        "content" (str refresh-period)}]]
    [:body
      [:h1 "RedDash"]
      [:h2 "Main Server"]
      (server-block "https://api.redmetrics.io" "https://redwire.io")
      [:h2 "Backup Server"]
      (server-block "http://api.redmetrics.crigamelab.org" "http://redwire.crigamelab.org")]))


(defroutes app-routes
  (GET "/" [] (use-hiccup main-page))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
