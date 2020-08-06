(ns status-im.ui.components.topbar
  (:require [re-frame.core :as re-frame]
            [quo.core :as quo]))

(def default-button-width 52)

(defn default-navigation [modal?]
  {:icon                (if modal? :main-icons/close :main-icons/arrow-left)
   :accessibility-label :back-button
   :on-press            #(re-frame/dispatch [:navigate-back])})

(defn topbar [{:keys [navigation use-insets right-accessories modal? content]
               :or   {use-insets true}
               :as   props}]
  (let [navigation (if (= navigation :none)
                     nil
                     [(merge (default-navigation modal?)
                             navigation)])]
    [quo/safe-area-consumer
     (fn [insets]
       [quo/header (merge {:left-accessories navigation
                           :title-component  content
                           :insets           (when use-insets insets)
                           :left-width       (when navigation
                                               default-button-width)}
                          props
                          (when (seq right-accessories)
                            {:right-accessories right-accessories}))])]))
