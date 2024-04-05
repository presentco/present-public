let ApiHandler = (function () {

  let getErrorMessage = function (serverResponse, defaultErrorMessage) {

        if (serverResponse.response && serverResponse.response.status === 400 && serverResponse.response.data && serverResponse.response.data.errorMsg) {
            return serverResponse.response.data.error.message;    //Get custom error message if any
        } else {
            return 'Oopps something went wrong';
        }
  };

//function to generetae uuid
  let randomUUID = function(){
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }

  //function to create uuid
  let guid = function() {
    return randomUUID() + randomUUID() + '-' + randomUUID() + '-' + randomUUID() + '-' +
      randomUUID() + '-' + randomUUID() + randomUUID() + randomUUID();
  }

  let attachHeader = function(data){

    let newSpace;
    try {
        const jsonCurrentSpace = localStorage.getItem('selectedSpace');
        if (jsonCurrentSpace !== undefined) {
            const selectedSpace = JSON.parse(jsonCurrentSpace);
            if (selectedSpace) {
                newSpace = selectedSpace.id
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }

    let request = {
      header: {
        clientUuid: localStorage.getItem('clientUuid'),
        requestUuid: guid(),
        authorizationKey: "not implemented",
        platform: "WEB",
        apiVersion: "0",
        spaceId: newSpace
      },
      argument: data
    };

    return request;
  }

  return {
    getErrorMessage,
    attachHeader,
    guid
  }

}());

export default ApiHandler;
