(defproject amperity/gocd-riemann-recorder "0.1.0-SNAPSHOT"
  :description "A plugin for GoCD that records things to Riemann."
  :url "https://github.com/amperity/gocd-riemann-recorder"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :repositories
  [["amperity" "https://s3-us-west-2.amazonaws.com/amperity-static-packages/jars/"]]

  :plugins
  [[lein-cloverage "1.1.0"]]

  :dependencies
  [[com.google.code.gson/gson "2.8.5"]
   [hiccup "1.0.5"]
   [org.clojure/clojure "1.10.1"]
   [riemann-clojure-client "0.5.1"]]

  :java-source-paths ["src"]

  :hiera
  {:cluster-depth 4
   :vertical false
   :show-external false}

  :profiles
  {:provided
   {:dependencies
    [[cd.go.plugin/go-plugin-api "19.8.0"]
     [com.google.guava/guava "23.0"]]}

   :repl
   {:source-paths ["dev"]
    :dependencies
    [[org.clojure/tools.namespace "0.2.11"]]}

   :uberjar
   {:target-path "target/uberjar"
    :aot :all}})
