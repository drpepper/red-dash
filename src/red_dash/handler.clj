(ns red-dash.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [clojure.core.async :refer [go-loop <! timeout]]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(def web-refresh-period 30) ; in seconds
(def server-refresh-period 30) ; in seconds
(def servers [{:name "RedMetrics-Main" :type :redmetrics :url "https://api.redmetrics.io"}
              {:name "RedWire-Main" :type :redwire :url "https://redwire.io"}
              {:name "RedMetrics-Backup" :type :redmetrics :url "http://api.redmetrics.crigamelab.org"}
              {:name "RedWire-Backup" :type :redwire :url "http://redwire.crigamelab.org"}])


; Maps each server name to a {:status, :down-since}
(def server-statuses (atom {}))

(def last-updated (atom nil))


(def time-formatter (f/formatters :rfc822))

; Old method: check status
; (defn check-redmetrics-server [url]
;   (let [{:keys [status body error]} @(http/get url)]
;     (if error
;         "ERROR contacting host"
;       (try 
;         (let [data (json/read-str body)] 
;           (if (= "1" (get data "apiVersion"))
;               :ok
;             "ERROR RedMetrics not returning valid data"))
;         (catch Exception e "ERROR parsing JSON")))))

; New method: check error code of /v1/game
(defn check-redmetrics-server [url]
  (let [{:keys [status body error]} @(http/get (str url "/v1/game"))]
    (if error
        "ERROR contacting host"
      (if (not= status 200)
          "ERROR with status"
        :ok)))) 

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

; Taken from https://stackoverflow.com/a/32511406 by user ma2s
(defn time-ago [time]
  (let [units [{:name "second" :limit 60 :in-second 1}
               {:name "minute" :limit 3600 :in-second 60}
               {:name "hour" :limit 86400 :in-second 3600}
               {:name "day" :limit 604800 :in-second 86400}
               {:name "week" :limit 2629743 :in-second 604800}
               {:name "month" :limit 31556926 :in-second 2629743}
               {:name "year" :limit nil :in-second 31556926}]
        diff (t/in-seconds (t/interval time (t/now)))]
    (if (< diff 5)
        "just now"
      (let [unit (first (drop-while #(or (>= diff (:limit %))
                                      (not (:limit %))) 
                                    units))]
        (-> (/ diff (:in-second unit))
          Math/floor
          int
          (#(str % " " (:name unit) (when (> % 1) "s") " ago")))))))

(defn main-page [request]
  (html5 
   [:head 
    (include-css "styles.css")
    [:meta {"http-equiv" "refresh" 
            "content" (str web-refresh-period)}]]
   [:body
    [:h1 "RedDash"]
    [:p (str "Last updated at " (f/unparse time-formatter @last-updated))]
    (for [{:keys [name type url]} servers]
      (let [{:keys [status]} (get @server-statuses name)]
        [:h3 {:class (if (= :ok status) "ok" "error")}
         (str name " @ " url ":  ") (if (= :ok status) "OK" status)]))]))

(defroutes app-routes
  (GET "/" [] (use-hiccup main-page))
  (route/resources "/")
  (route/not-found "Not Found"))


(defn update-server-statuses! []
  (swap! server-statuses 
         (fn [prev-server-statuses]
           (into {} 
                 (for [{:keys [name type url]} servers]
                   (let [status (if (= type :redmetrics)
                                    (check-redmetrics-server url)
                                  (check-redwire-server url))
                         prev-status (get-in prev-server-statuses [name :status])]
                     [name {:status status}])))))
  (reset! last-updated (t/now)))

(defn run-update-loop! []
  (go-loop []
           (<! (timeout (* 1000 server-refresh-period)))
           (update-server-statuses!)
           (recur)))

(defn init! []
  (run-update-loop!))

(defn destroy! []
  (prn "Stopping..."))


(def app
  (wrap-defaults app-routes site-defaults))
