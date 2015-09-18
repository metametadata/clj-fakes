(ns unit.fixtures.protocols)

(defprotocol AnimalProtocol
  (speak [this] [this name] [this name1 name2])
  (eat [this food drink])
  (sleep [this]))

(defprotocol FileProtocol
  (save [this])
  (scan [this]))