/*
 * Copyright 2015 the original author or authors.
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

/* global glowroot, angular, moment */

glowroot.controller('JvmGcEventsCtrl', [
  '$scope',
  '$location',
  '$http',
  'httpErrors',
  'queryStrings',
  function ($scope, $location, $http, httpErrors, queryStrings) {

    $scope.showTableSpinner = 0;

    var appliedFilter;

    $scope.showTableOverlay = 0;
    $scope.showTableSpinner = 0;

    function updateGcEvents(deferred) {
      var query = angular.copy(appliedFilter);
      query.sortAttribute = $scope.sortAttribute;
      query.sortDirection = $scope.sortDirection;
      $scope.showTableOverlay++;
      if (!deferred) {
        // show table spinner if not triggered from refresh button or show more button
        $scope.showTableSpinner++;
      }
      $http.get('backend/jvm/gc-events?' + queryStrings.encodeObject(query))
          .success(function (data) {
            $scope.showTableOverlay--;
            if (!deferred) {
              $scope.showTableSpinner--;
            }
            $scope.moreAvailable = data.moreAvailable;
            $scope.gcEvents = data.records;
            if (deferred) {
              deferred.resolve();
            }
          })
          .error(httpErrors.handler($scope, deferred));
    }

    $scope.refreshButtonClick = function (deferred) {
      var midnight = new Date(appliedFilter.from).setHours(0, 0, 0, 0);
      if (midnight !== $scope.filterDate.getTime()) {
        // filterDate has changed
        filterFromToDefault = false;
        appliedFilter.from = $scope.filterDate.getTime() + (appliedFilter.from - midnight);
        appliedFilter.to = $scope.filterDate.getTime() + (appliedFilter.to - midnight);
      }
      angular.extend(appliedFilter, $scope.filter);
      updateLocation();
      updateGcEvents(deferred);
    };

    $scope.showMore = function (deferred) {
      // double each time, but don't double $scope.filter.limit so that normal limit will be used on next search
      appliedFilter.limit *= 2;
      updateGcEvents(deferred);
    };

    $scope.filterTextComparatorOptions = [
      {
        display: 'Begins with',
        value: 'begins'
      },
      {
        display: 'Equals',
        value: 'equals'
      },
      {
        display: 'Ends with',
        value: 'ends'
      },
      {
        display: 'Contains',
        value: 'contains'
      },
      {
        display: 'Does not contain',
        value: 'not_contains'
      }
    ];

    var filterFromToDefault;

    appliedFilter = {};
    appliedFilter.from = Number($location.search().from);
    appliedFilter.to = Number($location.search().to);
    // both from and to must be supplied or neither will take effect
    if (appliedFilter.from && appliedFilter.to) {
      $scope.filterDate = new Date(appliedFilter.from);
      $scope.filterDate.setHours(0, 0, 0, 0);
    } else {
      filterFromToDefault = true;
      var today = new Date();
      today.setHours(0, 0, 0, 0);
      $scope.filterDate = today;
      appliedFilter.from = $scope.filterDate.getTime();
      appliedFilter.to = appliedFilter.from + 24 * 60 * 60 * 1000;
    }
    appliedFilter.actionComparator = $location.search()['action-comparator'] || 'begins';
    appliedFilter.action = $location.search().action || '';
    appliedFilter.causeComparator = $location.search()['cause-comparator'] || 'begins';
    appliedFilter.cause = $location.search().cause || '';
    appliedFilter.collectorNameComparator = $location.search()['collector-name-comparator'] || 'begins';
    appliedFilter.collectorName = $location.search().collectorName || '';
    appliedFilter.limit = 25;

    $scope.sortAttribute = $location.search()['sort-attribute'] || 'startTime';
    $scope.sortDirection = $location.search()['sort-direction'] || 'desc';

    $scope.filter = angular.copy(appliedFilter);
    // need to remove from and to so they aren't copied back during angular.extend(appliedFilter, $scope.filter)
    delete $scope.filter.from;
    delete $scope.filter.to;

    $scope.$watch('filter.from', function (newValue, oldValue) {
      if (newValue !== oldValue) {
        filterFromToDefault = false;
      }
    });

    $scope.$watch('filter.to', function (newValue, oldValue) {
      if (newValue !== oldValue) {
        filterFromToDefault = false;
      }
    });

    function updateLocation() {
      var query = {};
      if (!filterFromToDefault) {
        query.from = appliedFilter.from;
        query.to = appliedFilter.to;
      }
      if (appliedFilter.action) {
        query['action-comparator'] = appliedFilter.actionComparator;
        query.action = appliedFilter.action;
      }
      if (appliedFilter.cause) {
        query['cause-comparator'] = appliedFilter.causeComparator;
        query.cause = appliedFilter.cause;
      }
      if (appliedFilter.collectorName) {
        query['collector-name-comparator'] = appliedFilter.collectorNameComparator;
        query.collectorName = appliedFilter.collectorName;
      }
      if ($scope.sortAttribute !== 'startTime' || $scope.sortDirection !== 'desc') {
        query['sort-attribute'] = $scope.sortAttribute;
        if ($scope.sortDirection !== 'desc') {
          query['sort-direction'] = $scope.sortDirection;
        }
      }
      $location.search(query).replace();
    }

    $scope.sort = function (attributeName) {
      if ($scope.sortAttribute === attributeName) {
        // switch direction
        if ($scope.sortDirection === 'desc') {
          $scope.sortDirection = 'asc';
        } else {
          $scope.sortDirection = 'desc';
        }
      } else {
        $scope.sortAttribute = attributeName;
        $scope.sortDirection = 'desc';
      }
      updateLocation();
      updateGcEvents();
    };

    $scope.sortIconClass = function (attributeName) {
      if ($scope.sortAttribute !== attributeName) {
        return '';
      }
      if ($scope.sortDirection === 'desc') {
        return 'caret';
      } else {
        return 'caret transaction-caret-reversed';
      }
    };

    $scope.formatDate = function(timestamp) {
      // TODO internationalize time format
      // TODO display date once multi date search is supported
      return moment(timestamp).format('h:mm:ss.SSS a (Z)');
    };

    updateGcEvents();
  }
]);
