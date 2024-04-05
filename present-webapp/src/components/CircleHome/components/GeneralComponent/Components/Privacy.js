import React, { Component} from 'react';
import _ from 'lodash';

class Privacy extends Component {

  constructor(props){
    super(props);
    this.state={}
  }


  render(){

    let discoverable, womanOnly, preapprove;
    let currentGroup = this.props.currentGroup,
        customText = "",
       customPreapprove = "";

    if(!_.isEmpty(currentGroup)){
      discoverable = currentGroup.discoverable;
      womanOnly = currentGroup.space.name === "Women Only";
      preapprove= currentGroup.preapprove;
    }

    if(discoverable){
      if(!womanOnly){
        customText = 'People nearby can find this circle.';
      } else {
        customText = 'Women nearby can find this circle.';
      }
    } else {
      customText = 'Hidden from search.';
    }

    if(preapprove === 'INVITE_ONLY'){
      customPreapprove = womanOnly ? "Women must be added or approved." : "Members must be added or approved.";
    } else if(preapprove === 'FRIENDS'){
      customPreapprove = womanOnly ? "Friends of the owner can join. Others women must be added or approved." : "Friends of the owner can join. Others must be added or approved.";
    } else if(preapprove === 'FRIENDS_OF_MEMBERS'){
      customPreapprove = womanOnly ? "Friends of members can join. Others women must be added or approved." : 'Friends of members can join. Others must be added or approved.';
    } else if(preapprove === 'ANYONE'){
      customPreapprove = womanOnly ? "Only women can join." : 'Anyone can join.';
    }

    return(
      <div className="description-container">
        <p className="title-gen">Privacy</p>
        <p className="description-text">{customText}</p>
        <p className="who-join-title"><strong>Who can join?</strong></p>
        <p className="description-text">{customPreapprove}</p>
      </div>
    )
  }
}

export default Privacy;
