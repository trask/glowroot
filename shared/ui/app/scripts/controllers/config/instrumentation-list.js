/*
 * Copyright 2012-2019 the original author or authors.
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

glowroot.controller('ConfigInstrumentationListCtrl', [
  '$scope',
  '$location',
  '$http',
  'queryStrings',
  'httpErrors',
  function ($scope, $location, $http, queryStrings, httpErrors) {

    if ($scope.hideMainContent()) {
      return;
    }

    $scope.queryString = function (instrumentation) {
      var query = {};
      if ($scope.agentId) {
        query.agentId = $scope.agentId;
      }
      query.id = instrumentation.id;
      return queryStrings.encodeObject(query);
    };

    $http.get('backend/config/instrumentation?agent-id=' + encodeURIComponent($scope.agentId))
        .then(function (response) {
          $scope.loaded = true;
          $scope.instrumentationWithConfig = [];
          var instrumentationWithNoConfig = [];
          angular.forEach(response.data, function (instrumentation) {
            if (instrumentation.hasConfig) {
              $scope.instrumentationWithConfig.push(instrumentation);
            } else {
              instrumentationWithNoConfig.push(instrumentation.name);
            }
          });
          $scope.instrumentationWithNoConfig = instrumentationWithNoConfig.join(', ');
        }, function (response) {
          httpErrors.handle(response);
        });
  }
]);
