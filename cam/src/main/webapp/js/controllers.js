// CONTROLLERS
camApp.controller('homeController', [
		'$scope',
		'$http',
        '$q',
        'ngDialog',
            function ($scope, $http,$q, $ngDialog) {

        $scope.columnDefs = [{
            "mDataProp": "asset",
            "aTargets": [0]
			}, {
            "mDataProp": "class",
            "aTargets": [1]
			}, {
            "mDataProp": "model",
            "aTargets": [2]
			}, {
            "mDataProp": "owner",
            "aTargets": [3]
			}, {
            "mDataProp": "created",
            "aTargets": [4]
			}, {
            "mDataProp": "action",
            "aTargets": [5]
			}];

        $scope.overrideOptions = {
            "bStateSave": true,
            "iCookieDuration": 2419200,
            /* 1 month */
            "bJQueryUI": true,
            "bPaginate": true,
            "bLengthChange": false,
            "bFilter": true,
            "bInfo": true,
            "bDestroy": true
        };
            
        // $scope.BACK_END_URL = 'http://161.27.159.61:8080/CAMService'; //TODO Address config.JSON
        //$scope.BACK_END_URL = 'http://192.168.62.211:8080/CAMService';
              //$scope.BACK_END_URL = 'http://192.168.62.200:8080/CAMService';
        $scope.BACK_END_URL = 'http://localhost:8080/CAMService';
        entityManager.init($scope, $http, $q);
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
                $scope.assetList = []
                $scope.newAssetVisible = false;
            }
        }
        
        $scope.openNewAssetModelPanel = function () {
					$ngDialog.open({
						template: 'pages/newAssetModel.htm',
						controller: 'newAssetModelController',
                        scope: $scope
					});
				};
        
		}]);

camApp.controller('detailController', [ '$scope', '$http', '$routeParams', '$location', '$q','ngDialog',
        function($scope, $http, $routeParams, $location,$q, $ngDialog) {
        if(isEmpty($routeParams.selectedAssetName)){
            $location.path('/');
        }   
            $scope.selectedAssetName = $routeParams.selectedAssetName;
           
            $scope.BACK_END_URL = 'http://localhost:8080/CAMService';
            entityManager.init($scope, $http, $q);
            
            entityManager.getAssetDetail($routeParams.selectedAssetName);
            
            $scope.retrieveSelectedAsset = function() {
                for (var i = 0; i < $scope.assetList.length; i++) {
                    if ($scope.assetList[i].asset == $scope.selectedAssetName) {
                        alert();
                    }
                }
            }

            $scope.assetDetailColumnDefs = [ {
                "mDataProp" : "type",
                "aTargets" : [ 0 ]
            },{
                "mDataProp" : "name",
                "aTargets" : [ 1 ]
            }, {
                "mDataProp" : "value",
                "aTargets" : [ 2 ]
            }, {
                "mDataProp" : "action",
                "aTargets" : [ 3 ]
            } ];

            $scope.assetDetailOverrideOptions = {
                "bStateSave" : true,
                "iCookieDuration" : 2419200, /* 1 month */
                "bJQueryUI" : true,
                "bPaginate" : true,
                "bLengthChange" : false,
                "bFilter" : true,
                "bInfo" : true,
                "bDestroy" : true,
                "fnCreatedRow" :  function (nTd, sData, oData, iRow, iCol) {
                    alert();
                        $compile(nTd)($scope);
                }
            };

             // funzioni di utilità
            
            $scope.formatAssetDetailTableRow = function(data) {
                var attribute = {};
                attribute.name = data.normalizedName;
                attribute.value = data.propertyValue;              ;
	           attribute.action = '<div><i data-toggle="tooltip" title="Delete property" class="fa fa-remove cam-table-button"></i> <button class="cam-table-button" ng-click="openAttributeDetailPanel(\''
                            + data.normalizedName+'\')'
                            + '"> <i data-toggle="tooltip" title="Open detail" class="fa fa-search cam-table-button"></i> </button>';
                    if(data.type == 'relationship')
                        attribute.type = '<i data-toggle="tooltip" title="relationship" class="fa fa-link" ><i/>';
                    else
                        attribute.type = '<i data-toggle="tooltip" title="relationship" class="fa fa-font" ><i/>';
                
                return attribute;
            };
            
              $scope.openNewAttributePanel = function () {
					$ngDialog.open({
						template: 'pages/newAttribute.htm',
						controller: 'newAttributeController',
                        scope: $scope
					});
				};
            
             $scope.openAttributeDetailPanel = function (attributeName) {
                 $scope.attributeName = attributeName;
					$ngDialog.open({
						template: 'pages/newAttribute.htm',
						controller: 'attributeDetailController',
                        scope: $scope
					});
				};
        
             $scope.openNewRelationshipPanel = function () {
					$ngDialog.open({
						template: 'pages/newRelationship.htm',
						controller: 'newRelationshipController',
                        scope: $scope
					});
				};
        
		

        } ]);

camApp.controller('newAssetModelController', [
		'$scope',
		'$http',
        '$q',
	    'ngDialog',
		function ($scope, $http,$q, $ngDialog) {

            $scope.newAssetModel = {
                   name: "",
                   className: $scope.currentNode.className,
                   ownerName : ""
                };
            
            $scope.closeNewAssetModelPanel = function () {  
                $ngDialog.close();
            }
            $scope.saveNewAssetModel = function () {  
              $http.post($scope.BACK_END_URL+'/models', $scope.newAssetModel).success(function(data, status) {
                  $scope.loadChildren();
                  $ngDialog.close();
              }).error(function(err) {
                   alert(err);
            });
            }
        } ]);

camApp.controller('newAttributeController', [
		'$scope',
		'$http',
        '$q',
	    'ngDialog',
		function ($scope, $http,$q, $ngDialog) {

            $scope.newAttribute = {
                   name: "",
                   value: "",
                   type : ""
                };
            
            $scope.closeNewAttributePanel = function () {  
                $ngDialog.close();
            }
            var urlFragment = '/assets/';
            
            if(isEmpty($scope.selectedAsset.model)){
              $scope.isModel = true;
            }else{
              $scope.isModel = false;
            }

            if($scope.isModel)
                 urlFragment = '/models/';
            $scope.saveNewAttribute = function () {  
              $http.post($scope.BACK_END_URL+urlFragment+$scope.selectedAssetName+'/attributes', $scope.newAttribute).success(function(data, status) {
                  entityManager.getAssetDetail($scope.selectedAssetName);
                 $ngDialog.close();
              }).error(function(err) {
                   alert(err);
                });
            }
        } ]);

camApp.controller('attributeDetailController', [
		'$scope',
		'$http',
        '$q',
	    'ngDialog',
       	function ($scope, $http,$q, $ngDialog) {

            if(isEmpty($scope.selectedAsset.model)){
              $scope.isModel = true;
            }else{
              $scope.isModel = false;
            }

            if($scope.isModel)
                 urlFragment = '/models/';
             $http.get($scope.BACK_END_URL+urlFragment+$scope.selectedAssetName+'/attributes/'+$scope.attributeName)
                 .success(function (data) {
                $scope.newAttribute={
                    name: data.normalizedName,
                    value: data.propertyValue,
                    type: data.propertyType
                }
                });
            
            $scope.closeNewAttributePanel = function () {  
                $ngDialog.close();
            }
            var urlFragment = '/assets/';
                    
            $scope.saveNewAttribute = function () {  
              $http.post($scope.BACK_END_URL+urlFragment+$scope.selectedAssetName+'/attributes', $scope.newAttribute).success(function(data, status) {
                  entityManager.getAssetDetail($scope.selectedAssetName);
                 $ngDialog.close();
              }).error(function(err) {
                   alert(err);
                });
            }
        } ]);

camApp.controller('newRelationshipController', [
		'$scope',
		'$http',
        '$q',
	    'ngDialog',
		function ($scope, $http,$q, $ngDialog) {

            $scope.newRelationship = {
                   name: "",
                   referredName: ""
            };
            
            $scope.closeNewRelationshipPanel = function () {  
                $ngDialog.close();
            }
            var urlFragment = '/assets/';
            
            if(isEmpty($scope.selectedAsset.model)){
              $scope.isModel = true;
            }else{
              $scope.isModel = false;
            }

            if($scope.isModel)
                 urlFragment = '/models/';
            $scope.saveNewRelationship = function () {  
              $http.post($scope.BACK_END_URL+urlFragment+$scope.selectedAssetName+'/relationships', $scope.newRelationship).success(function(data, status) {
              entityManager.getAssetDetail($scope.selectedAssetName);
              $ngDialog.close();
              }).error(function(err) {
                   alert(err);
                });
            }
        } ]);