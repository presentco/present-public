import React from 'react';
import {Router, Route} from 'react-router';
import CircleHome from './components/CircleHome';
import SignUp from './components/SignUp';
import Discovery from './components/Discovery';
import VerifyLink from './components/VerifyLink';
import UserProfile from './components/UserProfile';

let isUserSignedUp = false,
    userCanProcceed = false;
//If we have a user in our localstorage try to get the data from the server
try {
    const jsonCurrentUser = localStorage.getItem('currentUser');
    if (jsonCurrentUser) {
        const currentUser = JSON.parse(jsonCurrentUser)
        if (currentUser) {
            isUserSignedUp=true;
        }
    }
} catch (e) {
    // localStorage.removeItem('currentUser');
}


if(isUserSignedUp && localStorage.getItem('authResponse') === 'PROCEED'){
    userCanProcceed = true;
} else {
    localStorage.removeItem('clientUuid');
    localStorage.removeItem('currentUser');
}

const Routes = (props) => (
    <Router {...props}>
            <Route path="/app" component={userCanProcceed ? Discovery : SignUp}>
                <Route path="createCircle" component={userCanProcceed ? Discovery : SignUp}/>
            </Route>
            <Route path="/t/:categoryName" component={userCanProcceed ? Discovery : SignUp}></Route>
            <Route path="/g/:vanityId" component={userCanProcceed ? CircleHome : SignUp}></Route>
            <Route path="/u/:userId" component={userCanProcceed ? UserProfile : SignUp}></Route>
            <Route path="/v/:verifyId" component={userCanProcceed ? Discovery : VerifyLink}></Route>

    </Router>
);

export default Routes;
