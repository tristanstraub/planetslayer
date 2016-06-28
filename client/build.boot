#!/usr/bin/env boot
(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.385"]
                 [rum "0.9.0"]
                 [pandeiro/boot-http "0.7.3"]
                 [adzerk/boot-cljs "1.7.228-1"]
                 [adzerk/boot-reload "0.4.7"]
                 [sablono "0.7.1"]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [enlive "1.1.6"]
                 [org.clojure/data.json "0.2.6"]
                 [org.martinklepsch/boot-garden "1.3.0-0"]
                 ])

(require '[pandeiro.boot-http :refer [serve]])
(require '[adzerk.boot-cljs :refer [cljs]])
(require '[adzerk.boot-reload :refer [reload]])
(require '[org.martinklepsch.boot-garden :refer [garden]])

(deftask client []
  (comp (serve)
        (watch)
        (reload :on-jsload 'planetslayer.core/main)
        (garden :styles-var 'planetslayer.styles/style :output-to "css/style.css" :pretty-print true)
        (cljs)))
