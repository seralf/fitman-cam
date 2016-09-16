camApp.controller('homeController', [
    '$scope',
    'Scopes',
    '$http',
    '$routeParams',
    '$route',
    '$q',
    'ngDialog',
    '$timeout',
    function ($scope, Scopes, $http, $routeParams, $route, $q, $ngDialog, $timeout) {

        Scopes.store('homeController', $scope);
        entityManager.init($scope, $http, $q);

        $scope.init = function () {
            var classHistory = $scope.ancestorsList;
            var htmlNodeList = angular.element.find('.ng-pristine')
            for (var i in classHistory) {
                var classItemSelected = classHistory[i];
                simulateClick(htmlNodeList, classItemSelected);
            }

            function simulateClick(htmlNodeList, classItemSelected) {
                for (var j in htmlNodeList) {
                    var htmlNode = htmlNodeList[j];
                    if (classItemSelected == htmlNode.textContent) {
                        htmlNode.click();
                        $timeout(function () {
                            var htmlNodeListNew = angular.element.find('.ng-pristine');
                            if (htmlNodeList.length !== htmlNodeListNew.length)
                                simulateClick(htmlNodeListNew, classHistory[i++]);
                            else
                                return;
                        }, 100);
                    }
                }
            }
        }
        $timeout($scope.init, 1000); //TODO 

        if (!isEmpty($routeParams.className)) {
            setTimeout(function () { //CHIAMATA ASINCRONA PER RICARICARE GLI ASSET DELLA CLASSE
                $scope.currentNode = {};
                $scope.currentNode.className = $routeParams.className;
                entityManager.getAssets($routeParams.className);
                entityManager.getAncestorsList($routeParams.className);
                $scope.newAssetVisible = true;
            }, 0);
            $scope.newAssetVisible = true;
        }

        $scope.regexPattern = REGEX_PATTERN;
        $scope.invalidNameMsg = INVALID_NAME_MSG;
        $scope.nameIsMandatory = NAME_IS_MANDATORY_MSG;


        $scope.columnDefs = [{

            "mDataProp": "asset",
            "aTargets": [0]
        },
            {
                "mDataProp": "model",
                "aTargets": [1]
            }, {
                "mDataProp": "owner",
                "aTargets": [2]
            }, {
                "mDataProp": "created",
                "aTargets": [3]
            }, {
                "mDataProp": "action",
                "aTargets": [4]
            }];

        $scope.overrideOptions = {
            "bStateSave": true,
            "iCookieDuration": 2419200,
            /* 1 month */
            "bJQueryUI": true,
            "bPaginate": true,
            "bSort": false,
            "bLengthChange": false,
            "bFilter": true,
            "bInfo": true,
            "bDestroy": true
        };


        $scope.assetList = [];
        entityManager.getClasses();
        $scope.newAssetVisible = false;


        //funzioni di utilità
        $scope.loadChildren = function () {
            entityManager.getChildrenForClass($scope.currentNode.className);
        }

        $scope.loadAsset = function () {
            //				alert($scope.currentNode); //per recuperare il nodo da passare in input a servizio rest
            if ($scope.currentNode.className) {
                entityManager.getAssets($scope.currentNode.className);

                $scope.newAssetVisible = true;
            } else {
                $scope.assetList = [];
                $scope.newAssetVisible = false;
            }
        }

        $scope.openNewAssetModelPanel = function () {
            $http.get(BACK_END_URL_CONST + '/owners')
                .success(function (data) {
                    $scope.ownersList = [];
                    for (var i = 0; i < data.length; i++) {
                        $scope.ownersList.push(data[i].name);
                    }
                    $ngDialog.open({
                        template: 'pages/newAssetModel.htm',
                        controller: 'newAssetModelController',
                        scope: $scope
                    });
                })
                .error(function (error) {
                    $scope.ownersList = [];
                    openErrorPanel(error);
                });
        }

        $scope.openNewAssetPanel = function (selectedModel) {
            $scope.selectedModel = selectedModel;

            $http.get(BACK_END_URL_CONST + '/owners')
                .success(function (data) {
                    $scope.ownersList = [];
                    for (var i = 0; i < data.length; i++) {
                        $scope.ownersList.push(data[i].name);
                    }
                    $ngDialog.open({
                        template: 'pages/newAsset.htm',
                        controller: 'newAssetController',
                        scope: $scope
                    });
                })
                .error(function (error) {
                    $scope.ownersList = [];
                    openErrorPanel(error);
                });


        }

        $scope.changeBackground = function (ev) {
            $('.ownselector').each(
                function () {
                    $(this).removeClass('selected');
                    $(this).removeClass('ownselector');
                });
            ev.target.className += ' selected ownselector';
        }

        $scope.openRemoveAssetPanel = function (elementToDelete, typeToDelete) {
            $scope.elementToDelete = elementToDelete;
            $scope.typeToDelete = typeToDelete;
            $ngDialog.open({
                template: 'pages/confirmDelete.htm',
                controller: 'confirmDeleteController',
                scope: $scope
            });
        }
        $scope.openConfirmDeleteClass = function (node) {
            $scope.elementToDelete = node.className;
            $scope.typeToDelete = 'class';
            $ngDialog.open({
                template: 'pages/confirmDelete.htm',
                controller: 'confirmDeleteController',
                scope: $scope
            });
        }

        $scope.openAddChildPanel = function (node) {
            $scope.className = node.className;
            $scope.title = 'Add child class';
            $ngDialog.open({
                template: 'pages/newClass.htm',
                controller: 'newChildClassController',
                scope: $scope
            });
        }

        $scope.openMoveClassPanel = function (node) {
            $scope.className = node.className;
            $scope.title = 'Move class';
            $ngDialog.open({
                template: 'pages/newClass.htm',
                controller: 'moveClassController',
                scope: $scope
            });
        }

        $scope.openNewClassPanel = function () {
            $scope.title = 'Create class';
            $ngDialog.open({
                template: 'pages/newClass.htm',
                controller: 'newClassController',
                scope: $scope
            });
        }

        $scope.openErrorPanel = function (err) {
            $scope.errorMsg = err;
            $ngDialog.open({
                template: 'pages/error.htm',
                controller: 'openErrorController',
                scope: $scope
            });
        }

        $scope.backToHomeWithExpandedTree = function (className) {
            $scope.ancestorClassName = className;
            var deferred = $q.defer();
            entityManager.getAncestorsList(className, deferred);
            var promise = deferred.promise;
            promise.then(function (data) {
                $route.reload();
                $scope.init();
            }, function (error) {
                console.log(error);
            });

        }

        $scope.collapseAllTreeNodes = function () {
            $scope.classList.forEach(function (elem) {
                elem.collapsed = true;
            });
        }

        $scope.expandAllTreeNodes = function () {
            $scope.classList.forEach(function (elem) {
                elem.collapsed = false;
            })
        }

        $scope.expandAncestors = function (elem) {
            function search(array, name) {
                for (var i in array) {
                    if(array[i].className === elem)
                        array[i].collapsed = false;
                }
            }

            var deferred = $q.defer();
            entityManager.getAncestorsList(elem, deferred);
            var promise = deferred.promise;
            promise.then(function (data) {
                var dataStr = data + '';
                var ancestors = dataStr.split(',');
                for (var i = 0; i < ancestors.length - 2; i++) {
                    search($scope.classList, ancestors[i]);
                }
            }, function (error) {
                console.log(error);
            });


        }

    }]);