(ns amperity.gocd.riemann.recorder.record
  "Recorder implementation."
  (:require
    [amperity.gocd.riemann.recorder.logging :as log]
    [amperity.gocd.riemann.recorder.settings :as settings]
    [riemann.client :as riemann]))


(defn- success-status
  []
  {:status "success"})


(defn- failure-status
  [message]
  {:status "failure"
   :message message})


(defn- record*
  [data]
  (log/info (pr-str data))
  (let [client (riemann/tcp-client {:host (settings/get-riemann-host)
                                    :port (settings/get-riemann-port)})]
    (riemann/send-event client data)))


(defn stage-status
  [data]
  (try
    (let [pipeline-name (get-in data [:pipeline :name])
          pipeline-label (get-in data [:pipeline :label])
          stage-name (get-in data [:pipeline :stage :name])]
      (if (= (get-in data [:pipeline :stage :state]) "Building")
        (log/info "Not recording state for in-progress pipeline: %s (%s)/%s"
                  pipeline-name
                  pipeline-label
                  stage-name)
        (record* data))
      (success-status))
  (catch Exception ex
    (failure-status (.getMessage ex)))))
