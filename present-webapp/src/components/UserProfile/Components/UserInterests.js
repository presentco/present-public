import React, { Component,PropTypes} from 'react';
import _ from 'lodash';


class UserInterests extends Component {

  constructor(props){
    super(props);
    this.state={
      user: props.user
    }
  }


  getUserInterests(interests){
    let imgType;

    return(
      interests.map((interest, index) => {
        if(interest.toLowerCase() === "style & beauty"){
          interest = "Style";
        }

        if(interest === "Woman Owned") {
          imgType = require('../../../assets/images/woman-selected@2x.png');
        } else  {
          imgType = require('../../../assets/images/'+interest.toLowerCase().replace(/\s/g, "")+'-selected@2x.png')
        }

        return(
          <div className="padding-bottom-row"  key={index}>
            <img src={imgType}
              className={"intere-icon-circle sele"} alt={interest} />
            <span className="in-title-circle">{interest}</span>
          </div>
        )
      })
    )
  }

  render(){
    let user = this.props.user,
        interests=[];

        if(!_.isEmpty(user) && user.interests.length !== 0){
          interests = user.interests;
        }

    return(
      <div className="user-photo-contrainer user-bio-container">
        <p className="user-header">Interests</p>
        {this.getUserInterests(interests)}
      </div>
    )
  }
}

UserInterests.contextTypes = {
  router: PropTypes.object
};


export default UserInterests;
