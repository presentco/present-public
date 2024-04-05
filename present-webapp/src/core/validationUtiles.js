import _ from 'lodash';

let ValidationUtils = (function () {

  let validateEmail = function (email) {
    let re=/^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(email);
  };

  let fieldContainCharacter = function(field){
    return /\S/.test(field);
  }

  let camelCaseToTitleCase = function(field){
    let result = field.replace( /([A-Z])/g, " $1" ),
        finalResult = result.charAt(0) + result.slice(1).toLowerCase();
    return finalResult;
  }


  let commonErrorValidation = function(fields,errors){
    //this function goes through all the fields and do the common validations on them
    for (let key in fields) {

      let obj = fields[key];

      if(!fieldContainCharacter(obj)){
          errors[key] = "please enter a valid value";
        } else if(key === 'email' && !validateEmail(obj)){
          errors[key] = "please enter a valid "+ camelCaseToTitleCase(key) + " address";
        }
    }
    return errors;
  }


  let errorEmpty = function(errors){

    let checkTrue =[];
    for (let key in errors) {
      let obj = errors[key];
      if(obj === ""){
        checkTrue.push(true);
      } else {
        checkTrue.push(false);
      }
    }

    if(checkTrue.indexOf(false) > -1){
      return false;
    } else {
      return true;
    }

  }

  return {
    validateEmail,
    commonErrorValidation,
    errorEmpty
  }

}());

export default ValidationUtils;
