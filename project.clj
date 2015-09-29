(defproject untangled "0.1.0-SNAPSHOT"
  :description "An opinionated data model for use with (and following) the sensibilities of Quiescent."
  :url ""
  :license {:name "NAVIS"
            :url  "http://www.thenavisway.com"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [quiescent "0.2.0-RC2"]
                 [org.clojure/clojurescript "1.7.48"]
                 [smooth-spec "0.1.0-SNAPSHOT"]
                 [differ "0.2.1"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [lein-cljsbuild "1.1.0"]
                 ]

  :source-paths ["src" "spec"]
  
  :plugins [[lein-cljsbuild "1.1.0"]
            [untangled "0.1.0-SNAPSHOT"]
            [lein-figwheel "0.4.0"]]

  :cljsbuild {:builds
              [{:id           "test"
                :source-paths ["src" "dev" "spec"  "checkouts/smooth-spec/src"]
                :figwheel     {:on-jsload "cljs.user/on-load"}
                :compiler     {:main                 cljs.user
                               :output-to            "resources/public/js/test/test.js"
                               :output-dir           "resources/public/js/test/out"
                               :recompile-dependents true
                               :asset-path           "js/test/out"
                               :optimizations        :none}}]}

  :profiles {
             :dev {
                   :source-paths ["src" "test" "dev" "checkouts/smooth-spec/src"]
                   :dependencies [[leiningen "2.5.3"]
                                  [leiningen-core "2.5.3"]]
                   :repl-options {
                                  :init-ns clj.user
                                  :port    7001
                                  }
                   :env          {:dev true}
                   }
             }

  :figwheel {
             :server-port 3450
             :css-dirs ["resources/public/css"] })
