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

/* global glowroot, angular */

glowroot.controller('TransactionServiceCallsCtrl', [
  '$scope',
  '$http',
  '$location',
  'locationChanges',
  'charts',
  'modals',
  'queryStrings',
  'httpErrors',
  function ($scope, $http, $location, locationChanges, charts, modals, queryStrings, httpErrors) {

    $scope.$parent.activeTabItem = 'service-calls';

    if ($scope.hideMainContent()) {
      return;
    }

    $scope.page = {};

    $scope.showServiceCalls = false;
    $scope.showSpinner = 0;

    $scope.$watchGroup(['range.chartFrom', 'range.chartTo', 'range.chartRefresh'], function () {
      refreshData();
    });

    $scope.$watch('page.serviceCallDest', function () {
      if ($scope.page.serviceCallDest !== '___all___') {
        $location.search('service-call-dest', $scope.page.serviceCallDest);
      } else {
        $location.search('service-call-dest', null);
      }
    });

    $scope.sortQueryString = function (attributeName) {
      var query = $scope.buildQueryObject();
      if (attributeName !== 'total-time' || ($scope.sortAttribute === 'total-time' && !$scope.sortAsc)) {
        query['sort-attribute'] = attributeName;
      }
      if ($scope.sortAttribute === attributeName && !$scope.sortAsc) {
        query['sort-direction'] = 'asc';
      }
      if ($scope.page.serviceCallDest !== '___all___') {
        query['service-call-dest'] = $scope.page.serviceCallDest;
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
      }
      $scope.page.serviceCallDest = $location.search()['service-call-dest'];
      if (!$scope.page.serviceCallDest) {
        $scope.page.serviceCallDest = '___all___';
      }
    });

    function refreshData() {
      var query = {
        agentRollupId: $scope.agentRollupId,
        transactionType: $scope.transactionType,
        transactionName: $scope.transactionName,
        from: $scope.range.chartFrom,
        to: $scope.range.chartTo
      };
      $scope.showSpinner++;
      $http.get('backend/transaction/service-calls' + queryStrings.encodeObject(query))
          .then(function (response) {
            $scope.showSpinner--;
            $scope.serviceCallsMap = {
              ___all___: []
            };
            $scope.limitExceededBucketMap = {};
            var data = response.data;
            if (data.overwritten) {
              $scope.showOverwrittenMessage = true;
              $scope.showServiceCalls = false;
              return;
            }
            $scope.showServiceCalls = data.length;
            var serviceCalls = data;
            var serviceCallDests = {};
            angular.forEach(serviceCalls, function (serviceCall) {
              serviceCall.timePerExecution = serviceCall.totalDurationNanos / (1000000 * serviceCall.executionCount);
              if (serviceCallDests[serviceCall.dest] === undefined) {
                serviceCallDests[serviceCall.dest] = 0;
              }
              serviceCallDests[serviceCall.dest] += serviceCall.totalDurationNanos;
            });
            $scope.serviceCallDests = Object.keys(serviceCallDests);
            $scope.serviceCallDests.sort(function (left, right) {
              return serviceCallDests[right] - serviceCallDests[left];
            });
            var mergedServiceCalls = {};
            angular.forEach(serviceCalls, function (serviceCall) {
              function newMergedServiceCall(serviceCall) {
                var mergedServiceCall = angular.copy(serviceCall);
                delete mergedServiceCall.dest;
                return mergedServiceCall;
              }

              function mergeServiceCall(mergedServiceCall, serviceCall) {
                mergedServiceCall.totalDurationNanos += serviceCall.totalDurationNanos;
                mergedServiceCall.executionCount += serviceCall.executionCount;
                mergedServiceCall.totalRows += serviceCall.totalRows;
              }

              if (serviceCall.text === 'LIMIT EXCEEDED BUCKET') {
                $scope.limitExceededBucketMap[serviceCall.dest] = serviceCall;
                if ($scope.limitExceededBucketMap.___all___) {
                  mergeServiceCall($scope.limitExceededBucketMap.___all___, serviceCall);
                } else {
                  $scope.limitExceededBucketMap.___all___ = newMergedServiceCall(serviceCall);
                }
              } else {
                var serviceCallsForDest = $scope.serviceCallsMap[serviceCall.dest];
                if (!serviceCallsForDest) {
                  serviceCallsForDest = [];
                  $scope.serviceCallsMap[serviceCall.dest] = serviceCallsForDest;
                }
                serviceCallsForDest.push(serviceCall);
                var mergedServiceCall = mergedServiceCalls[serviceCall.text];
                if (mergedServiceCall) {
                  mergeServiceCall(mergedServiceCall, serviceCall);
                } else {
                  mergedServiceCall = newMergedServiceCall(serviceCall);
                  mergedServiceCalls[serviceCall.text] = mergedServiceCall;
                  $scope.serviceCallsMap.___all___.push(mergedServiceCall);
                }
              }
            });

            if ($scope.page.serviceCallDest && $scope.page.serviceCallDest !== '___all___'
                && $scope.serviceCallDests.indexOf($scope.page.serviceCallDest) === -1) {
              $scope.serviceCallDests.push($scope.page.serviceCallDest);
            }
          }, function (response) {
            $scope.showSpinner--;
            httpErrors.handle(response);
          });
    }
  }
]);
