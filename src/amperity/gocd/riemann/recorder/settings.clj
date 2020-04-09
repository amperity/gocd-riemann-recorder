(ns amperity.gocd.riemann.recorder.settings
  (:require
    [amperity.gocd.riemann.recorder.logging :as log]
    [amperity.gocd.riemann.recorder.util :as u]
    [clojure.spec.alpha :as s]
    [hiccup.core :as hiccup])
  (:import
    (com.thoughtworks.go.plugin.api
      GoApplicationAccessor
      GoPluginIdentifier)
    (com.thoughtworks.go.plugin.api.request
      DefaultGoApiRequest)))


(def ^:private plugin-identifier
  "Identifier for the type of plugin and compatible API versions."
  (GoPluginIdentifier. "notification" ["4.0"]))


(s/def :gocd.plugin.setting/display-name string?)
(s/def :gocd.plugin.setting/display-order string?) ; should be a string containing an integer
(s/def :gocd.plugin.setting/default-value string?)
(s/def :gocd.plugin.setting/required boolean?)
(s/def :gocd.plugin.setting/secure boolean?)


(s/def :gocd.plugin.setting/setting-key keyword?)
(s/def :gocd.plugin.setting/setting
  (s/keys :req-un [:gocd.plugin.setting/display-name
                   :gocd.plugin.setting/display-order]
          :opt-un [:gocd.plugin.setting/default-value
                   :gocd.plugin.setting/required
                   :gocd.plugin.setting/secure]))
(s/def ::settings (s/map-of :gocd.plugin.setting/setting-key
                            :gocd.plugin.setting/setting))


;; The top-level keys have to be snek_case because angular won't save them
;; otherwise.
(def ^:const settings
  {:riemann_host {:display-name "Riemann Server Hostname" :display-order "0"}
   :riemann_port {:display-name "Riemann Server Port" :display-order "1"}})


(def ^:private state (atom nil))


(declare fetch-config!)


(defn initialize!
  [go-accessor]
  (s/valid? ::settings settings)
  (fetch-config! go-accessor)
  (log/info (pr-str @state)))


;; This only works for string config values currently.
(defn- setting-kv->view-hiccup
  [[k v]]
  [:div.form_item_block
   [:label (str (:display-name v) ":")
    (when (:required v)
      [:span.asterix "*"])]
   [:input {:type "text"
            :ng-model (name k)
            :ng-required (str (:required v))}]])


(defn get-view
  []
  (let [result-html (->> settings
                         (vec)
                         (sort-by #(Integer/parseInt (:display-order (second %))))
                         (into [:div] (map setting-kv->view-hiccup))
                         (hiccup/html))]
    {:template result-html}))


(defn get-configuration
  []
  settings)


;; TODO: implement this
(defn validate-configuration
  [data]
  [])


(defn plugin-settings-changed!
  "Handler for the go.plugin-settings.plugin-settings-changed event.
  Sets the saved config to the input value."
  [data]
  (reset! state data))


(defn fetch-config!
  "Fetches the current config value from the server and sets the saved config to
  that value."
  [go-accessor]
  (let [request (DefaultGoApiRequest. "go.processor.plugin-settings.get" "1.0" plugin-identifier)
        response (.submit go-accessor request)]
    (when (= 200 (.responseCode response))
      (reset! state (u/json-decode-map (.responseBody response))))))


(defn get-riemann-host
  "Returns the currently configured Riemann server hostname. May not be called
  before `initialize!`."
  []
  (:riemann_host @state))


(defn get-riemann-port
  "Returns the currently configured Riemann server port. May not be called
  before `initialize!`."
  []
  (:riemann_port @state))
