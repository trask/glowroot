/*
 * Copyright 2015-2019 the original author or authors.
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

/* global glowroot, HandlebarsRendering, angular, $, gtClipboard */

glowroot.controller('TransactionQueriesCtrl', [
  '$scope',
  '$http',
  '$location',
  '$timeout',
  '$filter',
  'locationChanges',
  'charts',
  'modals',
  'queryStrings',
  'httpErrors',
  function ($scope, $http, $location, $timeout, $filter, locationChanges, charts, modals, queryStrings, httpErrors) {

    $scope.$parent.activeTabItem = 'queries';

    if ($scope.hideMainContent()) {
      return;
    }

    $scope.page = {};

    $scope.showQueries = false;
    $scope.showSpinner = 0;
    $scope.showModalSpinner = 0;

    // these are needed for handling opening a direct link to a modal query
    var firstLocation = true;
    var firstLocationModalQueryText;
    var firstLocationModalQueryTextSha1;
    var firstLocationModalQueryFormat;


    $scope.$watchGroup(['range.chartFrom', 'range.chartTo', 'range.chartRefresh'], function () {
      refreshData();
    });

    $scope.$watch('page.queryDest', function () {
      if ($scope.page.queryDest !== '___all___') {
        $location.search('query-dest', $scope.page.queryDest);
      } else {
        $location.search('query-dest', null);
      }
    });

    $scope.smallScreen = function () {
      // using innerWidth so it will match to screen media queries
      return window.innerWidth < 1200;
    };

    $scope.sortQueryString = function (attributeName) {
      var query = $scope.buildQueryObject();
      if (attributeName !== 'total-time' || ($scope.sortAttribute === 'total-time' && !$scope.sortAsc)) {
        query['sort-attribute'] = attributeName;
      }
      if ($scope.sortAttribute === attributeName && !$scope.sortAsc) {
        query['sort-direction'] = 'asc';
      }
      if ($scope.page.queryDest !== '___all___') {
        query['query-dest'] = $scope.page.queryDest;
      }
      return queryStrings.encodeObject(query);
    };

    $scope.sortIconClass = function (attributeName) {
      if ($scope.sortAttribute !== attributeName) {
        return '';
      }
      if ($scope.sortAsc) {
        return 'gt-caret gt-caret-sort-ascending';
      } else {
        return 'gt-caret';
      }
    };

    $scope.ngAttrAriaSort = function (sortAttribute) {
      if (sortAttribute !== $scope.sortAttribute) {
        return undefined;
      }
      return $scope.sortAsc ? 'ascending' : 'descending';
    };

    var originalFrom = $location.search().from;
    var originalTo = $location.search().to;
    if (originalFrom !== undefined && originalTo !== undefined) {
      var dataPointIntervalMillis = charts.getDataPointIntervalMillis(originalFrom, originalTo,
          $scope.layout.queryAndServiceCallRollupExpirationMillis);
      var revisedFrom = Math.floor(originalFrom / dataPointIntervalMillis) * dataPointIntervalMillis;
      var revisedTo = Math.ceil(originalTo / dataPointIntervalMillis) * dataPointIntervalMillis;
      $location.search('from', revisedFrom);
      $location.search('to', revisedTo);
      $location.replace();
    }

    locationChanges.on($scope, function () {
      $scope.sortAttribute = $location.search()['sort-attribute'] || 'total-time';
      $scope.sortAsc = $location.search()['sort-direction'] === 'asc';
      if ($scope.sortAttribute === 'total-time') {
        $scope.sortAttr = '-totalDurationNanos';
      } else if ($scope.sortAttribute === 'execution-count') {
        $scope.sortAttr = '-executionCount';
      } else if ($scope.sortAttribute === 'time-per-execution') {
        $scope.sortAttr = '-timePerExecution';
      } else if ($scope.sortAttribute === 'rows-per-execution') {
        $scope.sortAttr = '-rowsPerExecution';
      }
      $scope.page.queryDest = $location.search()['query-dest'];
      if (!$scope.page.queryDest) {
        $scope.page.queryDest = '___all___';
      }

      var modalQueryText = $location.search()['modal-query-text'];
      var modalQueryTextSha1 = $location.search()['modal-query-text-sha1'];
      var modalQueryFormat = $location.search()['modal-query-format'];
      if (firstLocationModalQueryText || firstLocationModalQueryTextSha1) {
        $location.search('modal-query-text', firstLocationModalQueryText);
        $location.search('modal-query-text-sha1', firstLocationModalQueryTextSha1);
        $location.search('modal-query-format', firstLocationModalQueryFormat);
        firstLocationModalQueryText = undefined;
        firstLocationModalQueryTextSha1 = undefined;
        firstLocationModalQueryFormat = undefined;
      } else if (modalQueryText || modalQueryTextSha1) {
        if (firstLocation) {
          $location.search('modal-query-text', null);
          $location.search('modal-query-text-sha1', null);
          $location.search('modal-query-format', null);
          $location.replace();
          firstLocationModalQueryText = modalQueryText;
          firstLocationModalQueryTextSha1 = modalQueryTextSha1;
          firstLocationModalQueryFormat = modalQueryFormat;
        } else {
          $('#queryModal').data('location-query', [
            'modal-query-text',
            'modal-query-text-sha1',
            'modal-query-format'
          ]);
          displayModal(modalQueryText, modalQueryTextSha1, modalQueryFormat);
        }
      } else {
        $('#queryModal').modal('hide');
      }
      firstLocation = false;
    });

    $scope.showQueryModal = function (query) {
      if (query.fullQueryTextSha1) {
        $location.search('modal-query-text-sha1', query.fullQueryTextSha1);
      } else {
        $location.search('modal-query-text', query.truncatedQueryText);
      }
      var index = query.dest.indexOf(' ');
      var destFirstWord;
      if (index === -1) {
        destFirstWord = query.dest;
      } else {
        destFirstWord = query.dest.substring(0, index);
      }
      var sqlDestFirstWords = ['SQL', 'HSQLDB', 'H2', 'PostgreSQL', 'Oracle'];
      if (sqlDestFirstWords.indexOf(destFirstWord) !== -1) {
        $location.search('modal-query-format', 'sql');
      }
    };

    function displayModal(modalQueryText, modalQueryTextSha1, modalQueryFormat) {
      // clear previous content
      var $unformattedQuery = $('#unformattedQuery');
      var $formattedQuery = $('#formattedQuery');
      $unformattedQuery.text('');
      $formattedQuery.html('');
      $scope.unformattedQuery = '';
      $scope.formattedQuery = '';
      $scope.showFormatted = false;
      $scope.queryExpired = false;
      $scope.queryError = false;

      var $modalDialog = $('#queryModal .modal-dialog');
      var $closeButton = $('#queryModal button.close');
      var $clipboardIcon = $('#queryModal .fa-clipboard');

      function clearCss() {
        $modalDialog.removeAttr('style');
        $closeButton.removeAttr('style');
        $clipboardIcon.removeAttr('style');
      }

      function applyCss() {
        var width = Math.max($formattedQuery.width() + 80, 500);
        // +141 is needed for IE9 (other browsers seemed ok at +140)
        if (width < $modalDialog.width()) {
          $modalDialog.css('width', width + 'px');
        }
      }

      // delay is to avoid flashing content when displaying blank modal briefly before full text has loaded
      var timer = $timeout(function () {
        clearCss();
        modals.display('#queryModal');
        applyCss();
      }, 200);

      function display(fullText) {
        if ($timeout.cancel(timer)) {
          modals.display('#queryModal');
        }
        clearCss();
        $scope.unformattedQuery = fullText;
        $scope.formattedQuery = '';
        $scope.showFormatted = false;
        $scope.queryExpired = false;
        $unformattedQuery.text($scope.unformattedQuery);
        $unformattedQuery.show();
        $formattedQuery.hide();
        $('#queryModal').find('.gt-clip').removeClass('d-none');

        gtClipboard($clipboardIcon, '#queryModal', function () {
          return $scope.showFormatted ? $scope.formattedQuery : $scope.unformattedQuery;
        });

        if (modalQueryFormat !== 'sql') {
          return;
        }

        var formatted = HandlebarsRendering.sqlPrettyPrint(fullText);
        if (typeof formatted === 'object') {
          applyCss();
          return;
        }
        $scope.formattedQuery = formatted;
        $scope.showFormatted = true;
        $formattedQuery.html($scope.formattedQuery);
        $unformattedQuery.hide();
        $formattedQuery.show();
        applyCss();
      }

      if (!modalQueryTextSha1) {
        display(modalQueryText);
        return;
      }

      var q = {
        agentRollupId: $scope.agentRollupId,
        fullTextSha1: modalQueryTextSha1
      };
      $scope.showModalSpinner++;
      $http.get('backend/transaction/full-query-text' + queryStrings.encodeObject(q))
          .then(function (response) {
            $scope.showModalSpinner--;
            if (response.data.expired) {
              $scope.queryExpired = true;
              return;
            }
            display(response.data.fullText);
          }, function () {
            $scope.showModalSpinner--;
            $scope.queryError = true;
          });
    }

    $scope.toggleFormatted = function () {
      $scope.showFormatted = !$scope.showFormatted;
      var $formattedQuery = $('#formattedQuery');
      var $unformattedQuery = $('#unformattedQuery');
      if ($scope.showFormatted) {
        $unformattedQuery.hide();
        $formattedQuery.show();
      } else {
        $unformattedQuery.show();
        $formattedQuery.hide();
      }
    };

    function refreshData() {
      var query = {
        agentRollupId: $scope.agentRollupId,
        transactionType: $scope.transactionType,
        transactionName: $scope.transactionName,
        from: $scope.range.chartFrom,
        to: $scope.range.chartTo
      };
      $scope.showSpinner++;
      $http.get('backend/transaction/queries' + queryStrings.encodeObject(query))
          .then(function (response) {
            $scope.showSpinner--;
            $scope.queriesMap = {
              ___all___: []
            };
            $scope.limitExceededBucketMap = {};
            var data = response.data;
            if (data.overwritten) {
              $scope.showOverwrittenMessage = true;
              $scope.showQueries = false;
              return;
            }
            $scope.showQueries = data.length;
            var queries = data;
            var queryDests = {};
            var maxTotalDurationNanos = 0;
            var maxExecutionCount = 0;
            var maxTimePerExecution = 0;
            var maxRowsPerExecution = 0;
            angular.forEach(queries, function (query) {
              query.timePerExecution = query.totalDurationNanos / (1000000 * query.executionCount);
              if (query.totalRows === undefined) {
                query.rowsPerExecution = undefined;
              } else {
                query.rowsPerExecution = query.totalRows / query.executionCount;
              }
              if (queryDests[query.dest] === undefined) {
                queryDests[query.dest] = 0;
              }
              queryDests[query.dest] += query.totalDurationNanos;
              maxTotalDurationNanos = Math.max(maxTotalDurationNanos, query.totalDurationNanos);
              maxExecutionCount = Math.max(maxExecutionCount, query.executionCount);
              maxTimePerExecution = Math.max(maxTimePerExecution, query.timePerExecution);
              maxRowsPerExecution = Math.max(maxRowsPerExecution, query.rowsPerExecution);
            });
            var otherColumnsLength = HandlebarsRendering.formatMillis(maxTotalDurationNanos / 1000000).length
                + $filter('number')(maxExecutionCount).length
                + HandlebarsRendering.formatMillis(maxTimePerExecution).length
                + HandlebarsRendering.formatCount(maxRowsPerExecution).length;
            var maxQueryTextLength = 98 - otherColumnsLength * 0.6;
            angular.forEach(queries, function (query) {
              if (query.truncatedQueryText.length > maxQueryTextLength) {
                query.text = query.truncatedQueryText.substring(0, maxQueryTextLength - 3) + '...';
              } else {
                query.text = query.truncatedQueryText;
              }
            });
            $scope.queryDests = Object.keys(queryDests);
            $scope.queryDests.sort(function (left, right) {
              return queryDests[right] - queryDests[left];
            });
            var mergedQueriesBySha1 = {};
            var mergedQueriesByQueryText = {};
            angular.forEach(queries, function (query) {
              function newMergedQuery(query) {
                // intentionally keeping the first 'dest' merged so it can used for modal-query-format
                return angular.copy(query);
              }

              function mergeQuery(mergedQuery, query) {
                mergedQuery.totalDurationNanos += query.totalDurationNanos;
                mergedQuery.executionCount += query.executionCount;
                mergedQuery.totalRows += query.totalRows;
              }

              if (query.text === 'LIMIT EXCEEDED BUCKET') {
                $scope.limitExceededBucketMap[query.dest] = query;
                if ($scope.limitExceededBucketMap.___all___) {
                  mergeQuery($scope.limitExceededBucketMap.___all___, query);
                } else {
                  $scope.limitExceededBucketMap.___all___ = newMergedQuery(query);
                }
              } else {
                var queriesForDest = $scope.queriesMap[query.dest];
                if (!queriesForDest) {
                  queriesForDest = [];
                  $scope.queriesMap[query.dest] = queriesForDest;
                }
                queriesForDest.push(query);
                var mergedQuery;
                if (query.fullQueryTextSha1) {
                  mergedQuery = mergedQueriesBySha1[query.fullQueryTextSha1];
                  if (mergedQuery) {
                    mergeQuery(mergedQuery, query);
                  } else {
                    mergedQuery = newMergedQuery(query);
                    mergedQueriesBySha1[query.fullQueryTextSha1] = mergedQuery;
                    $scope.queriesMap.___all___.push(mergedQuery);
                  }
                } else {
                  mergedQuery = mergedQueriesByQueryText[query.truncatedQueryText];
                  if (mergedQuery) {
                    mergeQuery(mergedQuery, query);
                  } else {
                    mergedQuery = newMergedQuery(query);
                    mergedQueriesByQueryText[query.fullQueryTextSha1] = mergedQuery;
                    $scope.queriesMap.___all___.push(mergedQuery);
                  }
                }
              }
            });

            if ($scope.page.queryDest && $scope.page.queryDest !== '___all___'
                && $scope.queryDests.indexOf($scope.page.queryDest) === -1) {
              $scope.queryDests.push($scope.page.queryDest);
            }
          }, function (response) {
            $scope.showSpinner--;
            httpErrors.handle(response);
          });
    }
  }
]);
