import axios from 'axios';
import Constants from './Constants';

//We are using AXIOS to manage API calls.
//This is where we are going to configure the default values of the API.
let Api = axios.create({
       baseURL: Constants.BASE_URL,
       timeout: 30000
    });

export default Api;
