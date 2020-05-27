(ns status-im.ui.screens.bootnodes-settings.edit-bootnode.views
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.toolbar :as toolbar]
            [status-im.ui.components.text-input.view :as text-input]
            [status-im.ui.components.tooltip.views :as tooltip]
            [status-im.ui.components.topbar :as topbar]
            [quo.core :as quo]
            [status-im.ui.screens.bootnodes-settings.edit-bootnode.styles
             :as
             styles]
            [status-im.utils.platform :as platform])
  (:require-macros [status-im.utils.views :as views]))

(defn delete-button [id]
  [react/touchable-highlight {:on-press #(re-frame/dispatch [:bootnodes.ui/delete-pressed id])}
   [react/view styles/button-container
    [react/view {:style               styles/delete-button
                 :accessibility-label :bootnode-delete-button}
     [react/text {:style styles/button-label}
      (i18n/label :t/delete)]]]])

(def qr-code
  [react/touchable-highlight {:on-press #(re-frame/dispatch [:qr-scanner.ui/scan-qr-code-pressed
                                                             {:title (i18n/label :t/add-bootnode)
                                                              :handler :bootnodes.callback/qr-code-scanned}])
                              :style    styles/qr-code}
   [react/view
    [vector-icons/icon :main-icons/qr {:color colors/blue}]]])

(views/defview edit-bootnode []
  (views/letsubs [manage-bootnode   [:get-manage-bootnode]
                  validation-errors [:manage-bootnode-validation-errors]]
    (let [url          (get-in manage-bootnode [:url :value])
          id           (get-in manage-bootnode [:id :value])
          name         (get-in manage-bootnode [:name :value])
          is-valid?    (empty? validation-errors)
          invalid-url? (contains? validation-errors :url)]
      [react/keyboard-avoiding-view {:flex 1}
       [topbar/topbar {:title (if id :t/bootnode-details :t/add-bootnode)}]
       [react/scroll-view {:keyboard-should-persist-taps :handled}
        [react/view styles/edit-bootnode-view
         [text-input/text-input-with-label
          {:label               (i18n/label :t/name)
           :placeholder         (i18n/label :t/specify-name)
           :accessibility-label :bootnode-name
           :style               styles/input
           :container           styles/input-container
           :default-value       name
           :on-change-text      #(re-frame/dispatch [:bootnodes.ui/input-changed :name %])
           :auto-focus          true}]
         [react/view
          {:flex 1}
          [text-input/text-input-with-label
           (merge
            {:label               (i18n/label :t/bootnode-address)
             :placeholder         (i18n/label :t/bootnode-format)
             :style               styles/input
             :accessibility-label :bootnode-address
             :container           styles/input-container
             :default-value       url
             :on-change-text      #(re-frame/dispatch [:bootnodes.ui/input-changed :url %])}
            (when-not platform/desktop? {:content qr-code}))]
          (when (and (not (string/blank? url)) invalid-url?)
            [tooltip/tooltip (i18n/label :t/invalid-format
                                         {:format (i18n/label :t/bootnode-format)})
             {:color        colors/red-light
              :font-size    12
              :bottom-value 25}])]
         (when id
           [delete-button id])]]
       [toolbar/toolbar
        {:right
         [quo/button
          {:type      :secondary
           :after     :main-icon/next
           :disabled  (not is-valid?)
           :on-press  #(re-frame/dispatch [:bootnodes.ui/save-pressed])}
          (i18n/label :t/save)]}]])))
