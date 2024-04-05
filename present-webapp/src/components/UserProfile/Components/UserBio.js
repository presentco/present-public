import React, { Component,PropTypes} from 'react';
import _ from 'lodash';
import Linkify from 'react-linkify';

class UserBio extends Component {

  constructor(props){
    super(props);
    this.state={
      user: props.user
    }
  }

  renderBio(){
    let characterCnt,
        bio = <div/>,
        user = this.props.user;

    if(user.bio){
      characterCnt = user.bio.length;
    }

    bio = (
        <p className={!this.state.showMore &&  characterCnt > 244 ? "description-text showMore" : "description-text "}>
          {user.bio && user.bio !== "" ?
            user.bio.split('\n').map((item, key) => {
              return <Linkify properties={{target: '_blank'}} key={key}><span className="bio-user-text">{item}<br/></span></Linkify>
            })
            : ""}
        </p>
    )

    return bio;
  }

  render(){
    let user = this.props.user,
        name = "",
        bio = "";

        if(!_.isEmpty(user)){
          name = user.name.first;
          bio = user.bio;
        }

    return(
      <div className="user-photo-contrainer user-bio-container">
        <p className="user-header">About {name}</p>
        {this.renderBio()}

        {bio.length > 244 ?
          <div className="text-center ">
            {!this.state.showMore ? <div className="show-more-container"></div> : <span/>}
            <button onClick={() => this.setState({showMore: !this.state.showMore})}
              className="show-more">{!this.state.showMore ? "Read More" : "Read Less"}
            </button>
          </div>
          :
          <span/>
        }
      </div>
    )
  }
}

UserBio.contextTypes = {
  router: PropTypes.object
};

export default UserBio;
