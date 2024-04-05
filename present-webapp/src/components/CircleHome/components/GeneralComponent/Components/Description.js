import React, { Component} from 'react';
import _ from 'lodash';
import Linkify from 'react-linkify';

class Description extends Component {

  constructor(props){
    super(props);
    this.state={}
  }

  onChangeInput(e){
    e.preventDefault();
    this.setState({description:e.target.value});
  }

  renderDescription(){

    let characterCnt,
        description = <div/>,
        currentGroup = this.props.currentGroup;

    if(currentGroup.description){
       characterCnt = currentGroup.description.length;
    }
    if(!_.isEmpty(currentGroup)){
      description = (
        <p className={!this.state.showMore &&  characterCnt > 244 ? "description-text showMore" : "description-text "}>
          {currentGroup.description ?
            currentGroup.description.split('\n').map((item, key) => {
              return <Linkify properties={{target: '_blank'}} key={key}><span>{item}<br/></span></Linkify>
            })
            : "Check out the chat for info!"}
        </p>
      );
    }
    return description;
  }

  render(){

    let characterCnt = 0,
        currentGroup = this.props.currentGroup;

    if(currentGroup.description){

       characterCnt = currentGroup.description.length;
    }

    return(
      <div className="description-container">
        <p className="title-gen">Description</p>
        {this.renderDescription()}
        {characterCnt > 244 ?
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

export default Description;
