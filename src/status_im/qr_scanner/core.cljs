(ns status-im.qr-scanner.core
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.router.core :as router]
            [status-im.navigation :as navigation]
            [status-im.utils.utils :as utils]
            [status-im.utils.fx :as fx]))

(fx/defn scan-qr-code
  {:events [::scan-code]}
  [_ opts]
  {:request-permissions-fx
   {:permissions [:camera]
    :on-allowed  #(re-frame/dispatch [:navigate-to :qr-scanner opts])
    :on-denied   (fn []
                   (utils/set-timeout
                     #(utils/show-popup (i18n/label :t/error)
                                        (i18n/label :t/camera-access-error))
                     50))}})

(fx/defn set-qr-code
  {:events [:qr-scanner.callback/scan-qr-code-success]}
  [{:keys [db]} opts data]
  (when-let [handler (:handler opts)]
    {:dispatch [handler data opts]}))

(fx/defn set-qr-code-cancel
  {:events [:qr-scanner.callback/scan-qr-code-cancel]}
  [cofx opts]
  (fx/merge cofx
            (navigation/navigate-back)
            (when-let [handler (:cancel-handler opts)]
              (fn [] {:dispatch [handler opts]}))))
