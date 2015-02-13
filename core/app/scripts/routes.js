/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global glowroot */

glowroot.config([
  '$provide',
  '$stateProvider',
  '$urlRouterProvider',
  function ($provide, $stateProvider, $urlRouterProvider) {
    var waitForLayout;
    if (window.layout) {
      waitForLayout = function () {
      };
    } else {
      // some controllers need to wait for layout when running under grunt serve
      waitForLayout = ['$q', '$rootScope', function ($q, $rootScope) {
        var deferred = $q.defer();
        var unregisterWatch = $rootScope.$watch('layout', function (value) {
          if (value) {
            deferred.resolve();
            unregisterWatch();
          }
        });
        return deferred.promise;
      }];
    }
    // overriding autoscroll=true behavior to scroll to the top of the page
    $provide.decorator('$uiViewScroll', [
      function () {
        return function () {
          window.scrollTo(0, 0);
        };
      }
    ]);
    $urlRouterProvider.otherwise('transaction/overview');
    $stateProvider.state('transaction', {
      abstract: true,
      url: '/transaction?transaction-type',
      templateUrl: 'views/transaction.html',
      controller: 'TransactionCtrl',
      resolve: {
        headerDisplay: function () {
          return 'Transactions';
        },
        shortName: function () {
          return 'transaction';
        },
        defaultTabUrl: function () {
          return 'transaction/overview';
        },
        defaultSummarySortOrder: function () {
          return 'total-time';
        },
        // transaction controller needs to wait for layout when running under grunt serve
        waitForLayout: waitForLayout
      }
    });
    $stateProvider.state('transaction.detail', {
      abstract: true,
      views: {
        header: {
          templateUrl: 'views/transaction/header.html',
          controller: 'TransactionHeaderCtrl'
        },
        sidebar: {
          templateUrl: 'views/transaction/sidebar.html',
          controller: 'TransactionSidebarCtrl',
          resolve: {
            summarySortOrders: function () {
              return {
                'total-time': 'By percent of total time',
                'average-time': 'By average time',
                'throughput': 'By throughput (per min)'
              };
            },
            summaryValueFn: function () {
              return function (summary, sortOrder, overallSummary, durationMillis) {
                if (sortOrder === 'total-time') {
                  return (100 * summary.totalMicros / overallSummary.totalMicros).toFixed(1) + ' %';
                } else if (sortOrder === 'average-time') {
                  return (summary.totalMicros / (1000 * summary.transactionCount)).toFixed(1) + ' ms';
                } else if (sortOrder === 'throughput') {
                  return (60 * 1000 * summary.transactionCount / durationMillis).toFixed(1) + '/min';
                }
              };
            }
          }
        },
        tabs: {
          templateUrl: 'views/transaction/tabs.html',
          controller: 'TransactionTabCtrl'
        }
      }
    });
    $stateProvider.state('transaction.detail.overview', {
      url: '/overview?transaction-name',
      views: {
        'main@transaction': {
          templateUrl: 'views/transaction/overview.html',
          controller: 'TransactionOverviewCtrl'
        }
      }
    });
    $stateProvider.state('transaction.detail.metrics', {
      url: '/metrics?transaction-name',
      views: {
        'main@transaction': {
          templateUrl: 'views/transaction/metrics.html',
          controller: 'TransactionMetricsCtrl'
        }
      }
    });
    $stateProvider.state('transaction.detail.profile', {
      url: '/profile?transaction-name',
      views: {
        'main@transaction': {
          templateUrl: 'views/transaction/profile.html',
          controller: 'TransactionProfileCtrl'
        }
      }
    });
    $stateProvider.state('transaction.detail.traces', {
      url: '/traces?transaction-name',
      views: {
        'main@transaction': {
          templateUrl: 'views/transaction/traces.html',
          controller: 'TracesCtrl',
          resolve: {
            errorOnly: function () {
              return false;
            }
          }
        }
      }
    });
    $stateProvider.state('transaction-flame-graph', {
      url: '/transaction/flame-graph',
      templateUrl: 'views/transaction/flame-graph.html',
      controller: 'TransactionFlameGraphCtrl',
      resolve: {
        dummy: ['$q', '$timeout', function ($q, $timeout) {
          var deferred = $q.defer();

          function checkForD3() {
            if (window.d3) {
              deferred.resolve();
            } else {
              $timeout(checkForD3, 100);
            }
          }

          $timeout(checkForD3, 100);
          return deferred.promise;
        }],
        // flame graph controller needs to wait for layout when running under grunt serve
        waitForLayout: waitForLayout
      }
    });
    $stateProvider.state('error', {
      abstract: true,
      url: '/error?transaction-type',
      templateUrl: 'views/transaction.html',
      controller: 'TransactionCtrl',
      resolve: {
        headerDisplay: function () {
          return 'Errors';
        },
        shortName: function () {
          return 'error';
        },
        defaultTabUrl: function () {
          return 'error/messages';
        },
        defaultSummarySortOrder: function () {
          return 'error-count';
        },
        // error controller needs to wait for layout when running under grunt serve
        waitForLayout: waitForLayout
      }
    });
    $stateProvider.state('error.detail', {
      abstract: true,
      views: {
        header: {
          templateUrl: 'views/transaction/header.html',
          controller: 'TransactionHeaderCtrl'
        },
        sidebar: {
          templateUrl: 'views/transaction/sidebar.html',
          controller: 'TransactionSidebarCtrl',
          resolve: {
            summarySortOrders: function () {
              return {
                'error-count': 'By error count',
                'error-rate': 'By error rate'
              };
            },
            summaryValueFn: function () {
              return function (summary, sortOrder) {
                if (sortOrder === 'error-count') {
                  return summary.errorCount;
                } else if (sortOrder === 'error-rate') {
                  return (100 * summary.errorCount / summary.transactionCount).toFixed(1) + ' %';
                }
              };
            }
          }
        },
        tabs: {
          // same controller, just different html
          templateUrl: 'views/transaction/error-tabs.html',
          controller: 'TransactionTabCtrl'
        }
      }
    });
    $stateProvider.state('error.detail.messages', {
      url: '/messages?transaction-name',
      views: {
        'main@error': {
          templateUrl: 'views/transaction/error-messages.html',
          controller: 'ErrorMessagesCtrl'
        }
      }
    });
    $stateProvider.state('error.detail.traces', {
      url: '/traces?transaction-name',
      views: {
        'main@error': {
          templateUrl: 'views/transaction/traces.html',
          controller: 'TracesCtrl',
          resolve: {
            errorOnly: function () {
              return true;
            }
          }
        }
      }
    });
    $stateProvider.state('jvm', {
      url: '/jvm',
      templateUrl: 'views/jvm.html',
      controller: 'JvmCtrl'
    });
    $stateProvider.state('jvm.gauges', {
      url: '/gauges',
      templateUrl: 'views/jvm/gauges.html',
      controller: 'JvmGaugesCtrl',
      // gauges controller needs to wait for layout when running under grunt serve
      resolve: {
        waitForLayout: waitForLayout
      }
    });
<<<<<<< HEAD
    $stateProvider.state('jvm.mbeanTree', {
      url: '/mbean-tree',
      templateUrl: 'views/jvm/mbean-tree.html',
      controller: 'JvmMBeanTreeCtrl'
=======
    $stateProvider.state('jvm.gcEvents', {
      url: '/gc-events',
      templateUrl: 'views/jvm/gc-events.html',
      controller: 'JvmGcEventsCtrl'
    });
    $stateProvider.state('jvm.mbeans', {
      url: '/mbeans',
      templateUrl: 'views/jvm/mbeans.html',
      controller: 'JvmMBeansCtrl'
>>>>>>> ca4860f... GC Events
    });
    $stateProvider.state('jvm.threadDump', {
      url: '/thread-dump',
      templateUrl: 'views/jvm/thread-dump.html',
      controller: 'JvmThreadDumpCtrl'
    });
    $stateProvider.state('jvm.heapDump', {
      url: '/heap-dump',
      templateUrl: 'views/jvm/heap-dump.html',
      controller: 'JvmHeapDumpCtrl'
    });
    $stateProvider.state('jvm.processInfo', {
      url: '/process-info',
      templateUrl: 'views/jvm/process-info.html',
      controller: 'JvmProcessInfoCtrl'
    });
    $stateProvider.state('jvm.systemProperties', {
      url: '/system-properties',
      templateUrl: 'views/jvm/system-properties.html',
      controller: 'JvmSystemPropertiesCtrl'
    });
    $stateProvider.state('jvm.capabilities', {
      url: '/capabilities',
      templateUrl: 'views/jvm/capabilities.html',
      controller: 'JvmCapabilitiesCtrl'
    });
    $stateProvider.state('config', {
      url: '/config',
      templateUrl: 'views/config.html',
      controller: 'ConfigCtrl'
    });
    $stateProvider.state('config.general', {
      url: '/general',
      templateUrl: 'views/config/general.html',
      controller: 'ConfigCommonCtrl',
      resolve: {
        backendUrl: function () {
          return 'backend/config/general';
        }
      }
    });
    $stateProvider.state('config.instrumentationList', {
      url: '/instrumentation-list',
      templateUrl: 'views/config/instrumentation-list.html',
      controller: 'ConfigInstrumentationListCtrl'
    });
    $stateProvider.state('config.instrumentation', {
      url: '/instrumentation?v',
      templateUrl: 'views/config/instrumentation.html',
      controller: 'ConfigInstrumentationCtrl'
    });
    $stateProvider.state('config.gaugeList', {
      url: '/gauge-list',
      templateUrl: 'views/config/gauge-list.html',
      controller: 'ConfigGaugeListCtrl'
    });
    $stateProvider.state('config.gauge', {
      url: '/gauge',
      templateUrl: 'views/config/gauge.html',
      controller: 'ConfigGaugeCtrl'
    });
    $stateProvider.state('config.alertList', {
      url: '/alert-list',
      templateUrl: 'views/config/alert-list.html',
      controller: 'ConfigAlertListCtrl'
    });
    $stateProvider.state('config.alert', {
      url: '/alert',
      templateUrl: 'views/config/alert.html',
      controller: 'ConfigAlertCtrl',
      // alert controller needs to wait for layout when running under grunt serve
      resolve: {
        waitForLayout: waitForLayout
      }
    });
    $stateProvider.state('config.pluginList', {
      url: '/plugin-list',
      templateUrl: 'views/config/plugin-list.html',
      controller: 'ConfigPluginListCtrl'
    });
    $stateProvider.state('config.plugin', {
      url: '/plugin?id',
      templateUrl: 'views/config/plugin.html',
      controller: 'ConfigPluginCtrl'
    });
    $stateProvider.state('config.advanced', {
      url: '/advanced',
      templateUrl: 'views/config/advanced.html',
      controller: 'ConfigCommonCtrl',
      resolve: {
        backendUrl: function () {
          return 'backend/config/advanced';
        }
      }
    });
    $stateProvider.state('config.smtp', {
      url: '/smtp',
      templateUrl: 'views/config/smtp.html',
      controller: 'ConfigSmtpCtrl'
    });
    $stateProvider.state('config.userRecording', {
      url: '/user-recording',
      templateUrl: 'views/config/user-recording.html',
      controller: 'ConfigCommonCtrl',
      resolve: {
        backendUrl: function () {
          return 'backend/config/user-recording';
        }
      }
    });
    $stateProvider.state('config.userInterface', {
      url: '/ui',
      templateUrl: 'views/config/user-interface.html',
      controller: 'ConfigUserInterfaceCtrl'
    });
    $stateProvider.state('config.storage', {
      url: '/storage',
      templateUrl: 'views/config/storage.html',
      controller: 'ConfigStorageCtrl'
    });
    $stateProvider.state('login', {
      url: '/login',
      templateUrl: 'views/login.html',
      controller: 'LoginCtrl',
      // login controller needs to wait for layout when running under grunt serve
      resolve: {
        waitForLayout: waitForLayout
      }
    });
  }
]);
