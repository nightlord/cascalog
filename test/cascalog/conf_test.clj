(ns cascalog.conf-test
  (:use clojure.test
        [cascalog api testing])
  (:require [clojure.string :as s]
            [cascalog.conf :as conf]
            [cascalog.util :as u]))

(def comma
  (partial s/join ","))

(def defaults
  (comma u/default-serializations))

(deftest test-jobconf-bindings
  (with-job-conf {"key" "val"}
    (is (= conf/*JOB-CONF*
           {"io.serializations" defaults, "key" "val"})))

  (with-job-conf {"key" ["val1" "val2"]}
    (is (= conf/*JOB-CONF*
           {"io.serializations" defaults, "key" "val1,val2"}))
    (with-job-conf {"key" ["val3"]}
      (is (= conf/*JOB-CONF*
             {"io.serializations" defaults, "key" "val3"}))))
  
  (with-job-conf {"io.serializations" "java.lang.String"}
    (is (= conf/*JOB-CONF*
           {"io.serializations" (comma [defaults "java.lang.String"])})))

  (with-serializations [String]
    (is (= conf/*JOB-CONF*
           {"io.serializations" (comma [defaults "java.lang.String"])})))

  (with-serializations [String]
    (with-job-conf {"io.serializations" "java.lang.String,SomeSerialization"}
      (is (= conf/*JOB-CONF*
             {"io.serializations"
              (comma [defaults "java.lang.String" "SomeSerialization"])})))))

(deftest kryo-serialization-test
  (with-job-conf
    {"cascading.kryo.serializations" "java.util.DoesntExist"
     "cascading.kryo.skip.missing" true
     "cascading.kryo.accept.all" true}
    (let [cal-tuple [[(java.util.GregorianCalendar.)]]]
      (test?<- cal-tuple [?a] (cal-tuple ?a))))
  (with-job-conf
    {"cascading.kryo.accept.all" false}
    (let [cal-tuple [[(java.util.GregorianCalendar.)]]]
      (is (thrown? RuntimeException
                   (??<- [?a] (cal-tuple ?a)))))))
